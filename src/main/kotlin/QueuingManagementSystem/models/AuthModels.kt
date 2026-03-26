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
    val result: QueuingManagementSystem.models.GlobalCredentialResponse
)

fun QueuingManagementSystem.models.LoginRequest.validateLoginRequest(): MutableList<QueuingManagementSystem.models.GlobalCredentialResponse> {
    val errors = mutableListOf<QueuingManagementSystem.models.GlobalCredentialResponse>()
    if (username.isBlank()) errors.add(
        _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "username is required"
        )
    )
    if (password.isBlank()) errors.add(
        _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "password is required"
        )
    )
    return errors
}
