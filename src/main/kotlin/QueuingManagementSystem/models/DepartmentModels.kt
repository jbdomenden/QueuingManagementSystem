package QueuingManagementSystem.models

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

fun QueuingManagementSystem.models.DepartmentRequest.validateDepartmentRequest(): MutableList<QueuingManagementSystem.models.GlobalCredentialResponse> {
    val errors = mutableListOf<QueuingManagementSystem.models.GlobalCredentialResponse>()
    if (code.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "department code is required"
        )
    )
    if (name.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "department name is required"
        )
    )
    return errors
}
