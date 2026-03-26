package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class WindowRequest(
    val id: Int? = null,
    val department_id: Int,
    val area_id: Int?,
    val code: String,
    val name: String,
    val is_active: Boolean = true
)

@Serializable
data class WindowQueueTypeAssignmentRequest(
    val window_id: Int,
    val queue_type_ids: List<Int>
)

@Serializable
data class WindowModel(
    val id: Int,
    val department_id: Int,
    val area_id: Int?,
    val code: String,
    val name: String,
    val is_active: Boolean
)

fun QueuingManagementSystem.models.WindowRequest.validateWindowRequest(): MutableList<QueuingManagementSystem.models.GlobalCredentialResponse> {
    val errors = mutableListOf<QueuingManagementSystem.models.GlobalCredentialResponse>()
    if (department_id <= 0) errors.add(
        _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "department_id is required"
        )
    )
    if (code.isBlank()) errors.add(
        _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "code is required"
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
