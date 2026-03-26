package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class QueueTypeRequest(
    val id: Int? = null,
    val department_id: Int,
    val name: String,
    val code: String,
    val prefix: String,
    val is_active: Boolean = true
)

@Serializable
data class QueueTypeModel(
    val id: Int,
    val department_id: Int,
    val name: String,
    val code: String,
    val prefix: String,
    val is_active: Boolean
)

fun QueuingManagementSystem.models.QueueTypeRequest.validateQueueTypeRequest(): MutableList<QueuingManagementSystem.models.GlobalCredentialResponse> {
    val errors = mutableListOf<QueuingManagementSystem.models.GlobalCredentialResponse>()
    if (department_id <= 0) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "department_id is required"
        )
    )
    if (name.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "name is required"
        )
    )
    if (code.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "code is required"
        )
    )
    if (prefix.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "prefix is required"
        )
    )
    return errors
}
