package QueuingManagementSystem.auth.services

import QueuingManagementSystem.auth.models.AuthPrincipal
import QueuingManagementSystem.auth.models.CreateQueueUserRequest
import QueuingManagementSystem.auth.models.QueueUser
import QueuingManagementSystem.auth.models.StaffCurrentUserResult
import QueuingManagementSystem.auth.models.StaffLoginResult
import QueuingManagementSystem.auth.repositories.UserRepository
import QueuingManagementSystem.models.GlobalCredentialResponse

class AuthService(
    private val userRepository: UserRepository = UserRepository(),
    private val jwtService: JwtService
) {
    fun login(email: String, password: String): StaffLoginResult {
        val user = userRepository.findByEmail(email)
            ?: return unauthorized("Invalid credentials")

        if (!user.isActive) return unauthorized("User is inactive")
        if (!PasswordCrypto.verifyPassword(password, user.passwordHash)) return unauthorized("Invalid credentials")

        userRepository.updateLastLogin(user.id)
        val principal = user.toPrincipal()
        return StaffLoginResult(
            accessToken = jwtService.generateToken(principal),
            forcePasswordChange = user.forcePasswordChange,
            principal = principal,
            result = GlobalCredentialResponse(200, true, "Login successful")
        )
    }

    fun changePassword(token: String, currentPassword: String, newPassword: String): GlobalCredentialResponse {
        val currentUser = getCurrentQueueUser(token) ?: return GlobalCredentialResponse(401, false, "Unauthorized")
        if (!currentUser.isActive) return GlobalCredentialResponse(403, false, "User is inactive")
        if (!PasswordCrypto.verifyPassword(currentPassword, currentUser.passwordHash)) {
            return GlobalCredentialResponse(400, false, "Current password is invalid")
        }

        val policyError = PasswordCrypto.validatePasswordPolicy(newPassword)
        if (policyError != null) return GlobalCredentialResponse(400, false, policyError)

        val changed = userRepository.updatePassword(currentUser.id, PasswordCrypto.hashPassword(newPassword), false)
        return GlobalCredentialResponse(if (changed) 200 else 400, changed, if (changed) "Password updated" else "Password update failed")
    }

    fun getCurrentUser(token: String): StaffCurrentUserResult? {
        val user = getCurrentQueueUser(token) ?: return null
        if (!user.isActive) return null
        return StaffCurrentUserResult(
            principal = user.toPrincipal(),
            forcePasswordChange = user.forcePasswordChange,
            result = GlobalCredentialResponse(200, true, "OK")
        )
    }

    fun bootstrapIfEmpty() {
        if (userRepository.count() > 0) return

        val defaults = listOf(
            Triple("superadmin@qms.local", "SUPERADMIN", "Super Admin"),
            Triple("departmentadmin@qms.local", "DEPARTMENT_ADMIN", "Department Admin"),
            Triple("supervisor@qms.local", "SUPERVISOR", "Supervisor"),
            Triple("moderator@qms.local", "MODERATOR", "Moderator"),
            Triple("handler@qms.local", "HANDLER", "Handler")
        )

        defaults.forEach { item ->
            val defaultPassword = "admin123"
            userRepository.create(
                CreateQueueUserRequest(
                    email = item.first,
                    passwordHash = PasswordCrypto.hashPassword(defaultPassword),
                    role = item.second,
                    fullName = item.third,
                    companyId = null,
                    departmentId = if (item.second == "SUPERADMIN") null else 1,
                    isActive = true,
                    forcePasswordChange = true
                )
            )
        }
    }

    private fun getCurrentQueueUser(token: String): QueueUser? {
        if (token.isBlank()) return null
        val decoded = jwtService.verifyToken(token) ?: return null
        val userId = decoded.getClaim("userId")?.asInt() ?: return null
        return userRepository.findById(userId)
    }

    private fun QueueUser.toPrincipal(): AuthPrincipal = AuthPrincipal(
        userId = id,
        email = email,
        fullName = fullName,
        role = role,
        companyId = companyId,
        departmentId = departmentId,
        authSource = "QUEUE_USERS"
    )

    private fun unauthorized(message: String) = StaffLoginResult(
        accessToken = "",
        forcePasswordChange = false,
        principal = AuthPrincipal(0, "", "", "", null, null, "QUEUE_USERS"),
        result = GlobalCredentialResponse(401, false, message)
    )
}
