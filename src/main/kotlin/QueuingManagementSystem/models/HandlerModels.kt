package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class HandlerRequest(
    val id: Int? = null,
    val user_id: Int,
    val department_id: Int,
    val is_active: Boolean = true
)

@Serializable
data class HandlerSessionRequest(
    val handler_id: Int,
    val window_id: Int
)

@Serializable
data class HandlerModel(
    val id: Int,
    val user_id: Int,
    val department_id: Int,
    val is_active: Boolean
)

fun QueuingManagementSystem.models.HandlerRequest.validateHandlerRequest(): MutableList<QueuingManagementSystem.models.GlobalCredentialResponse> {
    val errors = mutableListOf<QueuingManagementSystem.models.GlobalCredentialResponse>()
    if (user_id <= 0) errors.add(
        _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "user_id is required"
        )
    )
    if (department_id <= 0) errors.add(
        _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "department_id is required"
        )
    )
    return errors
}
