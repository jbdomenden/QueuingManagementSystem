package QueuingManagementSystem.config

import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID

object UserSeeder {
    private data class SeedUser(
        val username: String,
        val password: String,
        val role: String,
        val fullName: String
    )

    private val seedUsers = listOf(
        SeedUser("superadmin", "admin123", "SUPER_ADMIN", "Super Admin"),
        SeedUser("admin1", "admin123", "ADMIN", "Admin One"),
        SeedUser("moderator1", "mod123", "MODERATOR", "Moderator One"),
        SeedUser("supervisor1", "sup123", "SUPERVISOR", "Supervisor One"),
        SeedUser("handler1", "handler123", "HANDLER", "Handler One"),
        SeedUser("handler2", "handler123", "HANDLER", "Handler Two"),
        SeedUser("user1", "user123", "USER", "User One")
    )

    private val legacyRoleFallback = mapOf(
        "SUPER_ADMIN" to "SUPERADMIN",
        "ADMIN" to "DEPARTMENT_ADMIN",
        "MODERATOR" to "DEPARTMENT_ADMIN",
        "SUPERVISOR" to "DEPARTMENT_ADMIN",
        "HANDLER" to "HANDLER",
        "USER" to "DEPARTMENT_ADMIN"
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
        val passwordHash = BCrypt.hashpw(seed.password, BCrypt.gensalt(12))
        val authToken = UUID.randomUUID().toString()

        val insertSql = """
            INSERT INTO users(username, password_hash, full_name, role, department_id, auth_token, is_active)
            VALUES (?, ?, ?, ?, NULL, ?, true)
        """.trimIndent()

        try {
            connection.prepareStatement(insertSql).use { statement ->
                statement.setString(1, seed.username)
                statement.setString(2, passwordHash)
                statement.setString(3, seed.fullName)
                statement.setString(4, seed.role)
                statement.setString(5, authToken)
                statement.executeUpdate()
            }
        } catch (sqlError: SQLException) {
            val fallbackRole = legacyRoleFallback[seed.role] ?: throw sqlError
            connection.prepareStatement(insertSql).use { statement ->
                statement.setString(1, seed.username)
                statement.setString(2, passwordHash)
                statement.setString(3, seed.fullName)
                statement.setString(4, fallbackRole)
                statement.setString(5, UUID.randomUUID().toString())
                statement.executeUpdate()
            }
        }
    }
}
