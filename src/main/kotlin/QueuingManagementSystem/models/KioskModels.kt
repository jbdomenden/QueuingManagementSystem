package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class KioskRequest(
    val id: Int? = null,
    val department_id: Int,
    val name: String,
    val is_active: Boolean = true
)

@Serializable
data class KioskQueueTypeAssignmentRequest(
    val kiosk_id: Int,
    val queue_type_ids: List<Int>
)

@Serializable
data class KioskModel(
    val id: Int,
    val department_id: Int,
    val name: String,
    val is_active: Boolean
)

fun KioskRequest.validateKioskRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (department_id <= 0) errors.add(GlobalCredentialResponse(400, false, "department_id is required"))
    if (name.isBlank()) errors.add(GlobalCredentialResponse(400, false, "name is required"))
    return errors
}
