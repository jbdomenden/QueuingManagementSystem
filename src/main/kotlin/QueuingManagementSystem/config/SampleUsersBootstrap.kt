package QueuingManagementSystem.config

import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import java.util.UUID

object SampleUsersBootstrap {
    private data class SampleUser(
        val username: String,
        val password: String,
        val fullName: String,
        val role: String,
        val departmentId: Int?
    )

    private val accounts = listOf(
        SampleUser("superadmin", "admin123", "Super Admin", "SUPERADMIN", null),
        SampleUser("admin_level", "admin123", "Admin Level", "DEPARTMENT_ADMIN", 1),
        SampleUser("user_level", "user123", "User Level", "HANDLER", 1)
    )

    fun bootstrap() {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                accounts.forEach { upsertUser(connection, it) }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun upsertUser(connection: Connection, user: SampleUser) {
        val passwordHash = BCrypt.hashpw(user.password, BCrypt.gensalt(12))
        val resolvedDepartmentId = resolveDepartmentId(connection, user.departmentId)
        val sql = """
            INSERT INTO users(username, password_hash, full_name, role, department_id, auth_token, is_active)
            VALUES (?, ?, ?, ?, ?, ?, true)
            ON CONFLICT (username) DO UPDATE
            SET password_hash = EXCLUDED.password_hash,
                full_name = EXCLUDED.full_name,
                role = EXCLUDED.role,
                department_id = EXCLUDED.department_id,
                auth_token = EXCLUDED.auth_token,
                is_active = true
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, user.username)
            statement.setString(2, passwordHash)
            statement.setString(3, user.fullName)
            statement.setString(4, user.role)
            if (resolvedDepartmentId == null) statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, resolvedDepartmentId)
            statement.setString(6, UUID.randomUUID().toString())
            statement.executeUpdate()
        }
    }

    private fun resolveDepartmentId(connection: Connection, requestedDepartmentId: Int?): Int? {
        if (requestedDepartmentId == null) return null
        connection.prepareStatement("SELECT id FROM departments WHERE id = ? LIMIT 1").use { statement ->
            statement.setInt(1, requestedDepartmentId)
            statement.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        return null
    }
}
