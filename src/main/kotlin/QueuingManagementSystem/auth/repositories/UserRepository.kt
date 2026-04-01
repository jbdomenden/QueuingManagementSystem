package QueuingManagementSystem.auth.repositories

import QueuingManagementSystem.auth.models.CreateQueueUserRequest
import QueuingManagementSystem.auth.models.QueueUser
import QueuingManagementSystem.config.ConnectionPoolManager
import java.time.Instant

class UserRepository {
    fun findByEmail(email: String): QueueUser? {
        val sql = """
            SELECT id, email, password_hash, role, full_name, company_id, department_id, is_active, force_password_change, last_login_at, created_at, updated_at
            FROM queue_users
            WHERE lower(email) = lower(?)
            LIMIT 1
        """.trimIndent()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, email)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return rs.toQueueUser()
                }
            }
        }
    }

    fun findById(id: Int): QueueUser? {
        val sql = """
            SELECT id, email, password_hash, role, full_name, company_id, department_id, is_active, force_password_change, last_login_at, created_at, updated_at
            FROM queue_users
            WHERE id = ?
            LIMIT 1
        """.trimIndent()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, id)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return rs.toQueueUser()
                }
            }
        }
    }

    fun create(request: CreateQueueUserRequest): Int {
        val sql = """
            INSERT INTO queue_users(email, password_hash, role, full_name, company_id, department_id, is_active, force_password_change)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, request.email)
                statement.setString(2, request.passwordHash)
                statement.setString(3, request.role)
                statement.setString(4, request.fullName)
                if (request.companyId == null) statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, request.companyId)
                if (request.departmentId == null) statement.setNull(6, java.sql.Types.INTEGER) else statement.setInt(6, request.departmentId)
                statement.setBoolean(7, request.isActive)
                statement.setBoolean(8, request.forcePasswordChange)
                statement.executeQuery().use { rs ->
                    if (rs.next()) return rs.getInt("id")
                }
            }
        }
        return 0
    }

    fun updatePassword(id: Int, passwordHash: String, forcePasswordChange: Boolean): Boolean {
        val sql = """
            UPDATE queue_users
            SET password_hash = ?,
                force_password_change = ?,
                updated_at = NOW()
            WHERE id = ?
        """.trimIndent()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, passwordHash)
                statement.setBoolean(2, forcePasswordChange)
                statement.setInt(3, id)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun updateLastLogin(id: Int): Boolean {
        val sql = """
            UPDATE queue_users
            SET last_login_at = NOW(),
                updated_at = NOW()
            WHERE id = ?
        """.trimIndent()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, id)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun count(): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement("SELECT COUNT(*) AS total FROM queue_users").use { statement ->
                statement.executeQuery().use { rs ->
                    if (rs.next()) return rs.getInt("total")
                }
            }
        }
        return 0
    }

    private fun java.sql.ResultSet.toQueueUser(): QueueUser {
        val companyId = getInt("company_id").let { if (wasNull()) null else it }
        val departmentId = getInt("department_id").let { if (wasNull()) null else it }
        val lastLoginAt = getTimestamp("last_login_at")?.toInstant()
        val createdAt = getTimestamp("created_at")?.toInstant() ?: Instant.now()
        val updatedAt = getTimestamp("updated_at")?.toInstant() ?: createdAt

        return QueueUser(
            id = getInt("id"),
            email = getString("email"),
            passwordHash = getString("password_hash"),
            role = getString("role"),
            fullName = getString("full_name"),
            companyId = companyId,
            departmentId = departmentId,
            isActive = getBoolean("is_active"),
            forcePasswordChange = getBoolean("force_password_change"),
            lastLoginAt = lastLoginAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
