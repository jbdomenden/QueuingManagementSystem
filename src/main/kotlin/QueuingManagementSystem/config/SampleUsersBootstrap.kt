package QueuingManagementSystem.config

import QueuingManagementSystem.auth.services.PasswordCrypto
import org.slf4j.LoggerFactory

object SampleUsersBootstrap {
    private val logger = LoggerFactory.getLogger(SampleUsersBootstrap::class.java)

    fun bootstrap() {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO queue_users(email, password_hash, role, full_name, is_active, force_password_change)
                VALUES (?, ?, ?, ?, true, true)
                ON CONFLICT (email)
                DO UPDATE SET
                    password_hash = EXCLUDED.password_hash,
                    role = EXCLUDED.role,
                    full_name = EXCLUDED.full_name,
                    is_active = true,
                    force_password_change = true,
                    updated_at = NOW()
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, "superadmin@qms.local")
                statement.setString(2, PasswordCrypto.hashPassword("admin123"))
                statement.setString(3, "SUPER_ADMIN")
                statement.setString(4, "Super Admin")
                statement.executeUpdate()
            }
            logger.info("Ensured local superadmin account is present and active")
        }
    }
}
