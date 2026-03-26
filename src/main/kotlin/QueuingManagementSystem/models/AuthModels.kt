package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val user_id: Int,
    val full_name: String,
    val role: String,
    val department_id: Int?,
    val token: String,
    val result: GlobalCredentialResponse
)

fun LoginRequest.validateLoginRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (username.isBlank()) errors.add(GlobalCredentialResponse(400, false, "username is required"))
    if (password.isBlank()) errors.add(GlobalCredentialResponse(400, false, "password is required"))
    return errors
}
