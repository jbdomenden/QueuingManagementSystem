package QueuingManagementSystem.config

import QueuingManagementSystem.auth.services.PasswordCrypto
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.UUID

object SampleUsersBootstrap {
    private val logger = LoggerFactory.getLogger(SampleUsersBootstrap::class.java)

    private data class SampleUser(
        val username: String,
        val email: String,
        val password: String,
        val fullName: String,
        val usersRole: String,
        val queueUsersRole: String,
        val forcePasswordChange: Boolean
    )

    private val accounts = listOf(
        SampleUser("superadmin@qms.local", "superadmin@qms.local", "admin123", "Super Admin", "SUPERADMIN", "SUPERADMIN", true),
        SampleUser("departmentadmin@qms.local", "departmentadmin@qms.local", "admin123", "Department Admin", "DEPARTMENT_ADMIN", "DEPARTMENT_ADMIN", true),
        SampleUser("supervisor@qms.local", "supervisor@qms.local", "sup123", "Supervisor", "DEPARTMENT_ADMIN", "SUPERVISOR", true),
        SampleUser("moderator@qms.local", "moderator@qms.local", "mod123", "Moderator", "DEPARTMENT_ADMIN", "MODERATOR", true),
        SampleUser("handler@qms.local", "handler@qms.local", "handler123", "Handler", "HANDLER", "HANDLER", true)
    )

    fun bootstrap() {
        logger.info("Local bootstrap started")
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val defaultDepartmentId = ensureDefaultDepartment(connection)
                seedUsersTable(connection, defaultDepartmentId)
                seedQueueUsersTable(connection, defaultDepartmentId)
                connection.commit()
                logger.info("Local bootstrap completed")
            } catch (e: Exception) {
                connection.rollback()
                logger.error("Local bootstrap failed: {}", e.message, e)
                throw IllegalStateException("Sample user bootstrap failed: ${e.message}", e)
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun ensureDefaultDepartment(connection: Connection): Int {
        val insertSql = """
            INSERT INTO departments(code, name, is_active)
            VALUES ('LOCAL', 'Local Department', true)
            ON CONFLICT (code) DO NOTHING
        """.trimIndent()
        connection.prepareStatement(insertSql).use { it.executeUpdate() }

        connection.prepareStatement("SELECT id FROM departments WHERE code = 'LOCAL' LIMIT 1").use { statement ->
            statement.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }

        connection.prepareStatement("SELECT id FROM departments ORDER BY id LIMIT 1").use { statement ->
            statement.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }

        throw IllegalStateException("No department available for local user bootstrap")
    }

    private fun seedUsersTable(connection: Connection, departmentId: Int) {
        val hasRows = connection.prepareStatement("SELECT 1 FROM users LIMIT 1").use { statement ->
            statement.executeQuery().use { rs -> rs.next() }
        }
        if (hasRows) {
            logger.info("Local bootstrap: users table already has data, skipping users seed")
            return
        }

        val sql = """
            INSERT INTO users(username, password_hash, full_name, role, department_id, auth_token, is_active)
            VALUES (?, ?, ?, ?, ?, ?, true)
            ON CONFLICT (username) DO NOTHING
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            accounts.forEach { user ->
                statement.setString(1, user.username)
                statement.setString(2, BCrypt.hashpw(user.password, BCrypt.gensalt(12)))
                statement.setString(3, user.fullName)
                statement.setString(4, user.usersRole)
                if (user.usersRole == "SUPERADMIN") statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, departmentId)
                statement.setString(6, UUID.randomUUID().toString())
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun seedQueueUsersTable(connection: Connection, departmentId: Int) {
        val hasRows = connection.prepareStatement("SELECT 1 FROM queue_users LIMIT 1").use { statement ->
            statement.executeQuery().use { rs -> rs.next() }
        }
        if (hasRows) {
            logger.info("Local bootstrap: queue_users table already has data, skipping queue_users seed")
            return
        }

        val sql = """
            INSERT INTO queue_users(email, password_hash, role, full_name, department_id, is_active, force_password_change)
            VALUES (?, ?, ?, ?, ?, true, ?)
            ON CONFLICT (email) DO NOTHING
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            accounts.forEach { user ->
                statement.setString(1, user.email)
                statement.setString(2, PasswordCrypto.hashPassword(user.password))
                statement.setString(3, user.queueUsersRole)
                statement.setString(4, user.fullName)
                if (user.queueUsersRole == "SUPERADMIN") statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, departmentId)
                statement.setBoolean(6, user.forcePasswordChange)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }
}
