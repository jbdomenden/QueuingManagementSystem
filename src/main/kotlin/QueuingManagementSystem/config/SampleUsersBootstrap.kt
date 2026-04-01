package QueuingManagementSystem.config

import QueuingManagementSystem.auth.services.PasswordCrypto
import org.slf4j.LoggerFactory

object SampleUsersBootstrap {
    private val logger = LoggerFactory.getLogger(SampleUsersBootstrap::class.java)

    fun bootstrap() {
        ConnectionPoolManager.getConnection().use { connection ->
            val hasRows = connection.prepareStatement("SELECT 1 FROM queue_users LIMIT 1").use { statement ->
                statement.executeQuery().use { rs -> rs.next() }
            }
            if (hasRows) return

            connection.prepareStatement(
                """
                INSERT INTO queue_users(email, password_hash, role, full_name, is_active, force_password_change)
                VALUES (?, ?, ?, ?, true, true)
                ON CONFLICT (email) DO NOTHING
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, "superadmin@qms.local")
                statement.setString(2, PasswordCrypto.hashPassword("admin123"))
                statement.setString(3, "SUPER_ADMIN")
                statement.setString(4, "Super Admin")
                statement.executeUpdate()
            }
            logger.info("Seeded local superadmin account")
        }
    }
}
