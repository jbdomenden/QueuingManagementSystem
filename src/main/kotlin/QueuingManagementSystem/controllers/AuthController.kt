package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.*
import QueuingManagementSystem.queries.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.util.Date
import java.util.UUID

class AuthController(
    private val jwtSecret: String = "change-me-secret",
    private val jwtIssuer: String = "qms",
    private val jwtAudience: String = "qms-clients",
    private val jwtExpirationMinutes: Long = 480L,
    private val singleSessionEnforced: Boolean = false
) {

    data class ValidatedSession(
        val sessionId: String,
        val userId: Int,
        val username: String,
        val fullName: String,
        val role: String,
        val departmentId: Int?,
        val departmentScopes: List<Int>,
        val permissions: List<String>
    )

    fun login(username: String, password: String, ipAddress: String, userAgent: String, clientId: String?): LoginResponse {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                var userId = 0
                var passwordHash = ""
                var fullName = ""
                var role = ""
                var departmentId: Int? = null
                var active = false

                connection.prepareStatement(getUserByUsernameForLoginQuery).use { statement ->
                    statement.setString(1, username)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            userId = rs.getInt("id")
                            passwordHash = rs.getString("password_hash")
                            fullName = rs.getString("full_name")
                            role = rs.getString("role")
                            departmentId = rs.getInt("department_id").let { if (rs.wasNull()) null else it }
                            active = rs.getBoolean("is_active")
                        }
                    }
                }

                if (userId <= 0 || !active) {
                    auditFailedLogin(connection, username, ipAddress, userAgent, "USER_NOT_FOUND_OR_INACTIVE")
                    auditSessionLifecycle(connection, null, null, "FAILED_LOGIN", username, "USER_NOT_FOUND_OR_INACTIVE")
                    connection.commit()
                    return unauthorized()
                }

                val passwordValid = BCrypt.checkpw(password, passwordHash)
                if (!passwordValid) {
                    auditFailedLogin(connection, username, ipAddress, userAgent, "INVALID_PASSWORD")
                    auditLogin(connection, userId, username, false, ipAddress, userAgent, "INVALID_PASSWORD")
                    auditSessionLifecycle(connection, userId, departmentId, "FAILED_LOGIN", username, "INVALID_PASSWORD")
                    connection.commit()
                    return unauthorized()
                }

                val normalizedRole = role.uppercase().replace("_", "")
                val allowedRoles = setOf("SUPERADMIN", "ADMIN", "MODERATOR", "SUPERVISOR", "HANDLER", "USER", "DEPARTMENTADMIN")
                if (!allowedRoles.contains(normalizedRole)) {
                    auditFailedLogin(connection, username, ipAddress, userAgent, "ROLE_NOT_ALLOWED")
                    auditSessionLifecycle(connection, userId, departmentId, "FAILED_LOGIN", username, "ROLE_NOT_ALLOWED")
                    connection.commit()
                    return unauthorized()
                }

                val permissions = getPermissionsByUserId(connection, userId)
                if (singleSessionEnforced) {
                    connection.prepareStatement(revokeOtherActiveUserSessionsQuery).use { s ->
                        s.setInt(1, userId)
                        s.setString(2, "FORCED_LOGOUT")
                        s.executeUpdate()
                    }
                    auditSessionLifecycle(connection, userId, departmentId, "FORCED_LOGOUT", "all_active_sessions", "single_session_enforced")
                }

                val sessionId = UUID.randomUUID().toString()
                val tokenRef = UUID.randomUUID().toString()
                val tokenRefHash = sha256(tokenRef)
                val token = issueJwt(userId, username, permissions, sessionId, tokenRef)

                connection.prepareStatement(postUserSessionQuery).use { s ->
                    s.setString(1, sessionId)
                    s.setInt(2, userId)
                    s.setString(3, tokenRefHash)
                    s.setString(4, ipAddress)
                    s.setString(5, userAgent)
                    s.setString(6, clientId)
                    s.executeQuery().use { rs -> rs.next() }
                }
                auditLogin(connection, userId, username, true, ipAddress, userAgent, "LOGIN_SUCCESS")
                auditSessionLifecycle(connection, userId, departmentId, "LOGIN", sessionId, "login_success")
                connection.commit()

                return LoginResponse(
                    user_id = userId,
                    full_name = fullName,
                    role = role,
                    department_id = departmentId,
                    token = token,
                    permissions = permissions,
                    result = GlobalCredentialResponse(200, true, "Login successful")
                )
            } catch (e: Exception) {
                connection.rollback()
                return LoginResponse(0, "", "", null, "", emptyList(), GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun logout(token: String): Boolean {
        if (token.isBlank()) return false
        val decoded = verifyToken(token) ?: return false
        val sessionId = decoded.getClaim("sid").asString() ?: return false
        val tokenRef = decoded.id ?: return false
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(updateUserSessionLogoutBySessionIdQuery).use { statement ->
                    statement.setString(1, sessionId)
                    statement.setString(2, sha256(tokenRef))
                    val updated = statement.executeUpdate() > 0
                    if (updated) {
                        val validated = getValidatedSessionByToken(token, connection, touchLastSeen = false)
                        auditSessionLifecycle(connection, validated?.userId, validated?.departmentId, "LOGOUT", sessionId, "user_logout")
                    }
                    connection.commit()
                    return updated
                }
            } catch (e: Exception) {
                connection.rollback()
                return false
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun validateSession(token: String): ValidateSessionResponse {
        val validated = getValidatedSessionByToken(token)
        if (validated == null) {
            return ValidateSessionResponse(0, "", "", "", null, emptyList(), GlobalCredentialResponse(401, false, "Unauthorized"))
        }
        return ValidateSessionResponse(
            user_id = validated.userId,
            username = validated.username,
            full_name = validated.fullName,
            role = validated.role,
            department_id = validated.departmentId,
            permissions = validated.permissions,
            result = GlobalCredentialResponse(200, true, "OK")
        )
    }

    fun changePassword(token: String, currentPassword: String, newPassword: String, ipAddress: String, userAgent: String): GlobalCredentialResponse {
        val validated = getValidatedSessionByToken(token) ?: return GlobalCredentialResponse(401, false, "Unauthorized")

        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                var existingHash = ""
                connection.prepareStatement("SELECT password_hash FROM users WHERE id = ? AND is_active = true").use { s ->
                    s.setInt(1, validated.userId)
                    s.executeQuery().use { rs -> if (rs.next()) existingHash = rs.getString("password_hash") }
                }
                if (existingHash.isBlank() || !BCrypt.checkpw(currentPassword, existingHash)) {
                    auditFailedLogin(connection, validated.username, ipAddress, userAgent, "CHANGE_PASSWORD_INVALID_CURRENT")
                    connection.commit()
                    return GlobalCredentialResponse(400, false, "Current password is invalid")
                }

                val newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12))
                connection.prepareStatement(updateUserPasswordHashQuery).use { s ->
                    s.setString(1, newHash)
                    s.setInt(2, validated.userId)
                    s.executeUpdate()
                }

                if (singleSessionEnforced) {
                    connection.prepareStatement(revokeOtherActiveUserSessionsQuery).use { s ->
                        s.setInt(1, validated.userId)
                        s.setString(2, "FORCED_LOGOUT")
                        s.executeUpdate()
                    }
                    auditSessionLifecycle(connection, validated.userId, validated.departmentId, "FORCED_LOGOUT", "all_active_sessions", "password_change_single_session")
                }

                auditLogin(connection, validated.userId, validated.username, true, ipAddress, userAgent, "CHANGE_PASSWORD_SUCCESS")
                auditSessionLifecycle(connection, validated.userId, validated.departmentId, "PASSWORD_CHANGE", validated.userId.toString(), "CHANGE_PASSWORD_SUCCESS")
                connection.commit()
                return GlobalCredentialResponse(200, true, "Password updated")
            } catch (e: Exception) {
                connection.rollback()
                return GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun getValidatedSessionByToken(token: String): ValidatedSession? {
        ConnectionPoolManager.getConnection().use { connection ->
            return getValidatedSessionByToken(token, connection, touchLastSeen = true)
        }
    }

    fun getUserSessionByToken(token: String): UserSessionModel {
        val validated = getValidatedSessionByToken(token) ?: return UserSessionModel()
        return UserSessionModel(
            user_id = validated.userId,
            department_id = validated.departmentId,
            department_scopes = validated.departmentScopes,
            role = validated.role,
            token = token,
            permissions = validated.permissions
        )
    }

    private fun getValidatedSessionByToken(token: String, connection: java.sql.Connection, touchLastSeen: Boolean): ValidatedSession? {
        if (token.isBlank()) return null
        val decoded = verifyToken(token) ?: return null
        val sessionId = decoded.getClaim("sid").asString() ?: return null
        val tokenRef = decoded.id ?: return null

        connection.prepareStatement(getActiveSessionByRefWithUserQuery).use { statement ->
            statement.setString(1, sessionId)
            statement.setString(2, sha256(tokenRef))
            statement.executeQuery().use { rs ->
                if (rs.next()) {
                    if (touchLastSeen) {
                        connection.prepareStatement(touchSessionLastSeenByIdQuery).use { update ->
                            update.setString(1, sessionId)
                            update.executeUpdate()
                        }
                    }
                    val userId = rs.getInt("user_id")
                    val permissions = getPermissionsByUserId(connection, userId)
                    val scopeDepartments = getDepartmentScopesByUserId(connection, userId).ifEmpty {
                        validatedDepartmentScopeFallback(rs.getInt("department_id").let { if (rs.wasNull()) null else it })
                    }
                    return ValidatedSession(
                        sessionId = sessionId,
                        userId = userId,
                        username = rs.getString("username"),
                        fullName = rs.getString("full_name"),
                        role = rs.getString("role"),
                        departmentId = rs.getInt("department_id").let { if (rs.wasNull()) null else it },
                        departmentScopes = scopeDepartments,
                        permissions = permissions
                    )
                }
            }
        }

        if (decoded.expiresAt != null && decoded.expiresAt.before(Date())) {
            connection.prepareStatement(markSessionExpiredByIdQuery).use { statement ->
                statement.setString(1, sessionId)
                if (statement.executeUpdate() > 0) {
                    auditSessionLifecycle(connection, null, null, "SESSION_EXPIRED", sessionId, "jwt_expired")
                }
            }
        }

        return null
    }

    private fun issueJwt(userId: Int, username: String, permissions: List<String>, sessionId: String, tokenRef: String): String {
        val now = Date()
        val expires = Date(now.time + jwtExpirationMinutes * 60_000)
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withJWTId(tokenRef)
            .withClaim("sid", sessionId)
            .withClaim("uid", userId)
            .withClaim("username", username)
            .withClaim("permissions", permissions)
            .withIssuedAt(now)
            .withExpiresAt(expires)
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    private fun verifyToken(token: String) = try {
        JWT.require(Algorithm.HMAC256(jwtSecret)).withIssuer(jwtIssuer).withAudience(jwtAudience).build().verify(token)
    } catch (_: Exception) {
        null
    }

    private fun getPermissionsByUserId(connection: java.sql.Connection, userId: Int): List<String> {
        val permissions = mutableListOf<String>()
        connection.prepareStatement(getPermissionsByUserIdQuery).use { statement ->
            statement.setInt(1, userId)
            statement.executeQuery().use { rs -> while (rs.next()) permissions.add(rs.getString("code")) }
        }
        return permissions
    }

    private fun getDepartmentScopesByUserId(connection: java.sql.Connection, userId: Int): List<Int> {
        val scopes = mutableListOf<Int>()
        connection.prepareStatement(getUserDepartmentScopesQuery).use { statement ->
            statement.setInt(1, userId)
            statement.executeQuery().use { rs ->
                while (rs.next()) scopes.add(rs.getInt("department_id"))
            }
        }
        return scopes
    }

    private fun validatedDepartmentScopeFallback(defaultDepartmentId: Int?): List<Int> {
        return if (defaultDepartmentId == null) emptyList() else listOf(defaultDepartmentId)
    }

    private fun auditLogin(connection: java.sql.Connection, userId: Int, username: String, success: Boolean, ipAddress: String, userAgent: String, reason: String) {
        connection.prepareStatement(postLoginAuditQuery).use { statement ->
            statement.setInt(1, userId)
            statement.setString(2, username)
            statement.setBoolean(3, success)
            statement.setString(4, ipAddress)
            statement.setString(5, userAgent)
            statement.setString(6, reason)
            statement.executeUpdate()
        }
    }

    private fun auditFailedLogin(connection: java.sql.Connection, username: String, ipAddress: String, userAgent: String, reason: String) {
        connection.prepareStatement(postFailedLoginAuditQuery).use { statement ->
            statement.setString(1, username)
            statement.setString(2, ipAddress)
            statement.setString(3, userAgent)
            statement.setString(4, reason)
            statement.executeUpdate()
        }
    }

    private fun auditSessionLifecycle(connection: java.sql.Connection, userId: Int?, departmentId: Int?, action: String, entityId: String, reason: String) {
        connection.prepareStatement(postSessionLifecycleAuditQuery).use { statement ->
            if (userId == null) statement.setNull(1, java.sql.Types.INTEGER) else statement.setInt(1, userId)
            if (departmentId == null) statement.setNull(2, java.sql.Types.INTEGER) else statement.setInt(2, departmentId)
            statement.setString(3, action)
            statement.setString(4, "user_session")
            statement.setString(5, entityId)
            statement.setString(6, "{\"reason\":\"$reason\"}")
            statement.executeUpdate()
        }
    }

    private fun unauthorized(): LoginResponse {
        return LoginResponse(0, "", "", null, "", emptyList(), GlobalCredentialResponse(401, false, "Invalid credentials"))
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
