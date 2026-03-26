package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class TicketCreateRequest(
    val kiosk_id: Int,
    val queue_type_id: Int
)

@Serializable
data class TicketActionRequest(
    val handler_id: Int,
    val ticket_id: Int,
    val notes: String? = null
)

@Serializable
data class CallNextRequest(
    val handler_id: Int
)

@Serializable
data class TicketModel(
    val id: Int,
    val ticket_number: String,
    val department_id: Int,
    val queue_type_id: Int,
    val kiosk_id: Int?,
    val assigned_window_id: Int?,
    val assigned_handler_id: Int?,
    val status: String,
    val created_at: String,
    val called_at: String? = null,
    val completed_at: String? = null
)

fun TicketCreateRequest.validateTicketCreateRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (kiosk_id <= 0) errors.add(GlobalCredentialResponse(400, false, "kiosk_id is required"))
    if (queue_type_id <= 0) errors.add(GlobalCredentialResponse(400, false, "queue_type_id is required"))
    return errors
}
