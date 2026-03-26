package marlow.systems.queuingsystem.models

import kotlinx.serialization.Serializable

@Serializable
data class DepartmentRequest(
    val id: Int? = null,
    val code: String,
    val name: String,
    val is_active: Boolean = true
)

@Serializable
data class DepartmentModel(
    val id: Int,
    val code: String,
    val name: String,
    val is_active: Boolean
)

fun DepartmentRequest.validateDepartmentRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (code.isBlank()) errors.add(GlobalCredentialResponse(400, false, "department code is required"))
    if (name.isBlank()) errors.add(GlobalCredentialResponse(400, false, "department name is required"))
    return errors
}
