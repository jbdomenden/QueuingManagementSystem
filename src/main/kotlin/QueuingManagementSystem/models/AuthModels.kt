package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LogoutRequest(
    val token: String? = null
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class ValidateSessionResponse(
    val user_id: Int,
    val username: String,
    val full_name: String,
    val role: String,
    val department_id: Int?,
    val permissions: List<String>,
    val result: GlobalCredentialResponse
)

@Serializable
data class LoginResponse(
    val user_id: Int,
    val full_name: String,
    val role: String,
    val department_id: Int?,
    val token: String,
    val permissions: List<String> = emptyList(),
    val result: GlobalCredentialResponse
)

fun LoginRequest.validateLoginRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (username.isBlank()) errors.add(GlobalCredentialResponse(400, false, "username is required"))
    if (password.isBlank()) errors.add(GlobalCredentialResponse(400, false, "password is required"))
    return errors
}

fun ChangePasswordRequest.validateChangePasswordRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (currentPassword.isBlank()) errors.add(GlobalCredentialResponse(400, false, "currentPassword is required"))
    if (newPassword.length < 8) errors.add(GlobalCredentialResponse(400, false, "newPassword must be at least 8 characters"))
    return errors
}
