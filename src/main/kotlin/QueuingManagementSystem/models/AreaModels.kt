package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class AreaRequest(
    val id: Int? = null,
    val department_id: Int,
    val name: String,
    val is_active: Boolean = true
)

@Serializable
data class AreaModel(
    val id: Int,
    val department_id: Int,
    val name: String,
    val is_active: Boolean
)

fun QueuingManagementSystem.models.AreaRequest.validateAreaRequest(): MutableList<QueuingManagementSystem.models.GlobalCredentialResponse> {
    val errors = mutableListOf<QueuingManagementSystem.models.GlobalCredentialResponse>()
    if (department_id <= 0) errors.add(
        _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "department_id is required"
        )
    )
    if (name.isBlank()) errors.add(
        _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "name is required"
        )
    )
    return errors
}
