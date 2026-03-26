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

fun UserRequest.validateUserRequest(isCreate: Boolean): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (username.isBlank()) errors.add(GlobalCredentialResponse(400, false, "username is required"))
    if (full_name.isBlank()) errors.add(GlobalCredentialResponse(400, false, "full_name is required"))
    if (role.isBlank()) errors.add(GlobalCredentialResponse(400, false, "role is required"))
    if (isCreate && (password == null || password.isBlank())) errors.add(GlobalCredentialResponse(400, false, "password is required"))
    return errors
}
