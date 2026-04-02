package QueuingManagementSystem.controllers

import QueuingManagementSystem.auth.services.PasswordCrypto
import QueuingManagementSystem.common.ALL_ACCESS_KEYS
import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.*
import QueuingManagementSystem.queries.*
import java.security.SecureRandom

class StaffAccessController {
    fun listUsers(): List<ManagedUserModel> {
        val users = mutableListOf<ManagedUserModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getManagedUsersQuery).use { statement ->
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        val id = rs.getInt("id")
                        users.add(
                            ManagedUserModel(
                                id = id,
                                email = rs.getString("email"),
                                fullName = rs.getString("full_name"),
                                role = rs.getString("role"),
                                isActive = rs.getBoolean("is_active"),
                                forcePasswordChange = rs.getBoolean("force_password_change"),
                                companyId = rs.getInt("company_id").let { if (rs.wasNull()) null else it },
                                departmentId = rs.getInt("department_id").let { if (rs.wasNull()) null else it },
                                permissions = userPermissions(connection, id)
                            )
                        )
                    }
                }
            }
        }
        return users
    }

    fun getUserById(userId: Int): ManagedUserModel? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getManagedUserByIdQuery).use { statement ->
                statement.setInt(1, userId)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return ManagedUserModel(
                        id = rs.getInt("id"),
                        email = rs.getString("email"),
                        fullName = rs.getString("full_name"),
                        role = rs.getString("role"),
                        isActive = rs.getBoolean("is_active"),
                        forcePasswordChange = rs.getBoolean("force_password_change"),
                        companyId = rs.getInt("company_id").let { if (rs.wasNull()) null else it },
                        departmentId = rs.getInt("department_id").let { if (rs.wasNull()) null else it },
                        permissions = userPermissions(connection, userId)
                    )
                }
            }
        }
    }

    fun createUser(request: UserCreateRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(createManagedUserQuery).use { statement ->
                statement.setString(1, request.email.trim())
                statement.setString(2, request.fullName.trim())
                statement.setString(3, PasswordCrypto.hashPassword(request.password))
                statement.setString(4, request.role)
                statement.setBoolean(5, request.isActive)
                statement.setBoolean(6, request.forcePasswordChange)
                if (request.companyId == null) statement.setNull(7, java.sql.Types.INTEGER) else statement.setInt(7, request.companyId)
                if (request.departmentId == null) statement.setNull(8, java.sql.Types.INTEGER) else statement.setInt(8, request.departmentId)
                statement.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateUser(userId: Int, request: UserUpdateRequest): Boolean {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(updateManagedUserQuery).use { statement ->
                statement.setString(1, request.fullName)
                statement.setString(2, request.role)
                statement.setBoolean(3, request.isActive)
                statement.setBoolean(4, request.forcePasswordChange)
                if (request.companyId == null) statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, request.companyId)
                if (request.departmentId == null) statement.setNull(6, java.sql.Types.INTEGER) else statement.setInt(6, request.departmentId)
                statement.setInt(7, userId)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun updateStatus(userId: Int, isActive: Boolean): Boolean = ConnectionPoolManager.getConnection().use { connection ->
        connection.prepareStatement(updateManagedUserStatusQuery).use { statement ->
            statement.setBoolean(1, isActive)
            statement.setInt(2, userId)
            statement.executeUpdate() > 0
        }
    }

    fun updateRole(userId: Int, role: String): Boolean = ConnectionPoolManager.getConnection().use { connection ->
        connection.prepareStatement(updateManagedUserRoleQuery).use { statement ->
            statement.setString(1, role)
            statement.setInt(2, userId)
            statement.executeUpdate() > 0
        }
    }

    fun updateAccess(userId: Int, access: List<UserAccessAssignment>): Boolean {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(upsertUserAccessQuery).use { statement ->
                    access.forEach {
                        statement.setInt(1, userId)
                        statement.setString(2, it.accessKey)
                        statement.setBoolean(3, it.isAllowed)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.commit()
                return true
            } catch (e: Exception) {
                connection.rollback(); throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun resetPassword(userId: Int, password: String?): String {
        val newPassword = password?.takeIf { it.isNotBlank() } ?: generateTemporaryPassword()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement("UPDATE queue_users SET password_hash = ?, force_password_change = true, updated_at = NOW() WHERE id = ?").use { statement ->
                statement.setString(1, PasswordCrypto.hashPassword(newPassword))
                statement.setInt(2, userId)
                statement.executeUpdate()
            }
        }
        return newPassword
    }

    fun emailExists(email: String): Boolean = ConnectionPoolManager.getConnection().use { connection ->
        connection.prepareStatement("SELECT 1 FROM queue_users WHERE lower(email) = lower(?) LIMIT 1").use { statement ->
            statement.setString(1, email)
            statement.executeQuery().use { it.next() }
        }
    }

    private fun userPermissions(connection: java.sql.Connection, userId: Int): List<String> {
        val keys = mutableSetOf<String>()
        connection.prepareStatement(getUserAccessQuery).use { statement ->
            statement.setInt(1, userId)
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    val key = rs.getString("access_key")
                    if (rs.getBoolean("is_allowed")) keys.add(key) else keys.remove(key)
                }
            }
        }
        return keys.toList()
    }

    private fun generateTemporaryPassword(length: Int = 12): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#"
        val random = SecureRandom()
        return (1..length).joinToString("") { chars[random.nextInt(chars.length)].toString() }
    }

    fun isValidRole(role: String): Boolean = setOf("SUPER_ADMIN", "COMPANY_ADMIN", "MANAGER", "SUPERVISOR", "ACCOUNTING", "EMPLOYEE").contains(role)
    fun isValidAccessKey(key: String): Boolean = ALL_ACCESS_KEYS.contains(key)
}
