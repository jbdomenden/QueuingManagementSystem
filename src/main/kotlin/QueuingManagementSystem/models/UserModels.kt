package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val id: Int? = null,
    val username: String,
    val password: String? = null,
    val full_name: String,
    val role: String,
    val department_id: Int?,
    val is_active: Boolean = true
)

@Serializable
data class UserModel(
    val id: Int,
    val username: String,
    val full_name: String,
    val role: String,
    val department_id: Int?,
    val is_active: Boolean
)

fun QueuingManagementSystem.models.UserRequest.validateUserRequest(isCreate: Boolean): MutableList<QueuingManagementSystem.models.GlobalCredentialResponse> {
    val errors = mutableListOf<QueuingManagementSystem.models.GlobalCredentialResponse>()
    if (username.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "username is required"
        )
    )
    if (full_name.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "full_name is required"
        )
    )
    if (role.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "role is required"
        )
    )
    if (isCreate && (password == null || password.isBlank())) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "password is required"
        )
    )
    return errors
}
