package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.*
import QueuingManagementSystem.queries.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

class AuthController(
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String,
    private val jwtExpirationMinutes: Long,
    private val singleSessionEnforced: Boolean
) {

    data class ValidatedSession(
        val userId: Int,
        val username: String,
        val fullName: String,
        val departmentId: Int?,
        val permissions: List<String>
    )

    fun login(username: String, password: String, ipAddress: String, userAgent: String): LoginResponse {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                var userId = 0
                var passwordHash = ""
                var fullName = ""
                var departmentId: Int? = null
                var active = false

                connection.prepareStatement(getUserByUsernameForLoginQuery).use { statement ->
                    statement.setString(1, username)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            userId = rs.getInt("id")
                            passwordHash = rs.getString("password_hash")
                            fullName = rs.getString("full_name")
                            departmentId = rs.getInt("department_id").let { if (rs.wasNull()) null else it }
                            active = rs.getBoolean("is_active")
                        }
                    }
                }

                if (userId <= 0 || !active) {
                    auditFailedLogin(connection, username, ipAddress, userAgent, "USER_NOT_FOUND_OR_INACTIVE")
                    connection.commit()
                    return unauthorized()
                }

                val passwordValid = BCrypt.checkpw(password, passwordHash)
                if (!passwordValid) {
                    auditFailedLogin(connection, username, ipAddress, userAgent, "INVALID_PASSWORD")
                    auditLogin(connection, userId, username, false, ipAddress, userAgent, "INVALID_PASSWORD")
                    connection.commit()
                    return unauthorized()
                }

                val permissions = getPermissionsByUserId(connection, userId)
                if (singleSessionEnforced) {
                    connection.prepareStatement(deactivateUserSessionsQuery).use { s ->
                        s.setInt(1, userId)
                        s.executeUpdate()
                    }
                }

                val token = issueJwt(userId, username, permissions)
                connection.prepareStatement(postUserSessionQuery).use { s ->
                    s.setInt(1, userId)
                    s.setString(2, token)
                    s.setString(3, ipAddress)
                    s.setString(4, userAgent)
                    s.executeQuery().use { rs -> rs.next() }
                }
                auditLogin(connection, userId, username, true, ipAddress, userAgent, "LOGIN_SUCCESS")
                connection.commit()

                return LoginResponse(
                    user_id = userId,
                    full_name = fullName,
                    role = "",
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
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(updateUserSessionLogoutByTokenQuery).use { statement ->
                statement.setString(1, token)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun validateSession(token: String): ValidateSessionResponse {
        val validated = getValidatedSessionByToken(token)
        if (validated == null) {
            return ValidateSessionResponse(0, "", "", null, emptyList(), GlobalCredentialResponse(401, false, "Unauthorized"))
        }
        return ValidateSessionResponse(
            user_id = validated.userId,
            username = validated.username,
            full_name = validated.fullName,
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
                    connection.prepareStatement(deactivateUserSessionsQuery).use { s ->
                        s.setInt(1, validated.userId)
                        s.executeUpdate()
                    }
                }

                auditLogin(connection, validated.userId, validated.username, true, ipAddress, userAgent, "CHANGE_PASSWORD_SUCCESS")
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
        if (token.isBlank()) return null
        val decoded = try {
            JWT.require(Algorithm.HMAC256(jwtSecret)).withIssuer(jwtIssuer).withAudience(jwtAudience).build().verify(token)
        } catch (_: Exception) {
            return null
        }

        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getActiveSessionByTokenWithUserQuery).use { statement ->
                statement.setString(1, token)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        val userId = rs.getInt("user_id")
                        val permissions = getPermissionsByUserId(connection, userId)
                        return ValidatedSession(
                            userId = userId,
                            username = rs.getString("username"),
                            fullName = rs.getString("full_name"),
                            departmentId = rs.getInt("department_id").let { if (rs.wasNull()) null else it },
                            permissions = permissions
                        )
                    }
                }
            }
        }
        return null
    }

    fun getUserSessionByToken(token: String): UserSessionModel {
        val validated = getValidatedSessionByToken(token) ?: return UserSessionModel()
        return UserSessionModel(
            user_id = validated.userId,
            department_id = validated.departmentId,
            role = "",
            token = token,
            permissions = validated.permissions
        )
    }

    private fun issueJwt(userId: Int, username: String, permissions: List<String>): String {
        val now = Date()
        val expires = Date(now.time + jwtExpirationMinutes * 60_000)
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("uid", userId)
            .withClaim("username", username)
            .withClaim("permissions", permissions)
            .withIssuedAt(now)
            .withExpiresAt(expires)
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    private fun getPermissionsByUserId(connection: java.sql.Connection, userId: Int): List<String> {
        val permissions = mutableListOf<String>()
        connection.prepareStatement(getPermissionsByUserIdQuery).use { statement ->
            statement.setInt(1, userId)
            statement.executeQuery().use { rs -> while (rs.next()) permissions.add(rs.getString("code")) }
        }
        return permissions
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

    private fun unauthorized(): LoginResponse {
        return LoginResponse(0, "", "", null, "", emptyList(), GlobalCredentialResponse(401, false, "Invalid credentials"))
    }
}
