package QueuingManagementSystem.config

import QueuingManagementSystem.auth.services.PasswordCrypto
import org.slf4j.LoggerFactory
import java.sql.Connection

object SampleUsersBootstrap {
    private val logger = LoggerFactory.getLogger(SampleUsersBootstrap::class.java)

    private data class SampleUser(
        val email: String,
        val password: String,
        val fullName: String,
        val role: String,
        val departmentId: Int?,
        val forcePasswordChange: Boolean
    )

    private val accounts = listOf(
        SampleUser("superadmin@qms.local", "admin123", "Super Admin", "SUPERADMIN", null, true),
        SampleUser("departmentadmin@qms.local", "admin123", "Department Admin", "DEPARTMENT_ADMIN", 1, true),
        SampleUser("supervisor@qms.local", "sup123", "Supervisor", "SUPERVISOR", 1, true),
        SampleUser("moderator@qms.local", "mod123", "Moderator", "MODERATOR", 1, true),
        SampleUser("handler@qms.local", "handler123", "Handler", "HANDLER", 1, true)
    )

    fun bootstrap() {
        logger.info("Local bootstrap started")
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                if (queueUsersHasAnyRows(connection)) {
                    logger.info("Local bootstrap skipped because queue_users already has data")
                    connection.commit()
                    return
                }

                accounts.forEach { insertQueueUser(connection, it) }
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

    private fun queueUsersHasAnyRows(connection: Connection): Boolean {
        connection.prepareStatement("SELECT 1 FROM queue_users LIMIT 1").use { statement ->
            statement.executeQuery().use { rs -> return rs.next() }
        }
    }

    private fun insertQueueUser(connection: Connection, user: SampleUser) {
        val sql = """
            INSERT INTO queue_users(email, password_hash, role, full_name, department_id, is_active, force_password_change)
            VALUES (?, ?, ?, ?, ?, true, ?)
            ON CONFLICT (email) DO NOTHING
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, user.email)
            statement.setString(2, PasswordCrypto.hashPassword(user.password))
            statement.setString(3, user.role)
            statement.setString(4, user.fullName)
            if (user.departmentId == null) statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, user.departmentId)
            statement.setBoolean(6, user.forcePasswordChange)
            statement.executeUpdate()
        }
    }
}
