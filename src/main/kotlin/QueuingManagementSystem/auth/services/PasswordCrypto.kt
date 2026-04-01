package QueuingManagementSystem.auth.services

import QueuingManagementSystem.common.PasswordPolicy
import org.mindrot.jbcrypt.BCrypt

object PasswordCrypto {
    fun hashPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(12))

    fun verifyPassword(password: String, hash: String): Boolean =
        runCatching { BCrypt.checkpw(password, hash) }.getOrDefault(false)

    fun validatePasswordPolicy(password: String): String? = PasswordPolicy.validate(password, "password")
}
