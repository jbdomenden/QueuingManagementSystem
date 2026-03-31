package QueuingManagementSystem.config

import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import java.util.UUID

object UserSeeder {
    private val allowedHumanRoles = setOf(
        "SUPERADMIN",
        "DEPARTMENT_ADMIN",
        "HANDLER",
        "SUPER_ADMIN",
        "ADMIN",
        "MODERATOR",
        "SUPERVISOR",
        "USER"
    )

    private data class SeedUser(
        val username: String,
        val password: String,
        val role: String,
        val fullName: String,
        val departmentId: Int? = null
    )

    private val seedUsers = listOf(
        SeedUser("superadmin", "admin123", "SUPERADMIN", "Super Admin"),
        SeedUser("admin_level", "admin123", "DEPARTMENT_ADMIN", "Admin Level", 1),
        SeedUser("user_level", "user123", "HANDLER", "User Level", 1),
        SeedUser("admin1", "admin123", "DEPARTMENT_ADMIN", "Admin One", 1),
        SeedUser("handler1", "handler123", "HANDLER", "Handler One", 1),
        SeedUser("handler2", "handler123", "HANDLER", "Handler Two", 1)
    )

    fun seedUsers() {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                seedUsers.forEach { seed ->
                    if (!userExists(connection, seed.username)) {
                        insertSeedUser(connection, seed)
                    }
                }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun userExists(connection: Connection, username: String): Boolean {
        connection.prepareStatement("SELECT 1 FROM users WHERE username = ? LIMIT 1").use { statement ->
            statement.setString(1, username)
            statement.executeQuery().use { rs ->
                return rs.next()
            }
        }
    }

    private fun insertSeedUser(connection: Connection, seed: SeedUser) {
        require(seed.role in allowedHumanRoles) { "Unsupported seed role: ${seed.role}" }
        val passwordHash = BCrypt.hashpw(seed.password, BCrypt.gensalt(12))
        val authToken = UUID.randomUUID().toString()
        val resolvedDepartmentId = resolveDepartmentId(connection, seed.departmentId)

        val insertSql = """
            INSERT INTO users(username, password_hash, full_name, role, department_id, auth_token, is_active)
            VALUES (?, ?, ?, ?, ?, ?, true)
        """.trimIndent()

        connection.prepareStatement(insertSql).use { statement ->
            statement.setString(1, seed.username)
            statement.setString(2, passwordHash)
            statement.setString(3, seed.fullName)
            statement.setString(4, seed.role)
            if (resolvedDepartmentId == null) {
                statement.setNull(5, java.sql.Types.INTEGER)
            } else {
                statement.setInt(5, resolvedDepartmentId)
            }
            statement.setString(6, authToken)
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
