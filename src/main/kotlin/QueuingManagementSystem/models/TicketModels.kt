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
data class ArchiveDayRequest(
    val serviceDate: String,
    val departmentId: Int? = null
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
    val completed_at: String? = null,
    val queueDate: String? = null,
    val queueTime: String? = null,
    val queuedAt: String? = null,
    val waitingSeconds: Long? = null,
    val waitingDisplay: String? = null,
    val servedSeconds: Long? = null,
    val servedDisplay: String? = null
)

@Serializable
data class PrintableTicketModel(
    val ticketId: Int,
    val ticketNumber: String,
    val departmentId: Int,
    val departmentName: String,
    val companyName: String? = null,
    val queueTypeId: Int,
    val queueTypeName: String,
    val status: String,
    val queueDate: String,
    val queueTime: String,
    val queuedAt: String,
    val formattedPrintText: String
)

@Serializable
data class TicketCreateResponse(
    val ticket: TicketModel,
    val printableTicket: PrintableTicketModel,
    val result: GlobalCredentialResponse
)

@Serializable
data class ArchivedTicketModel(
    val id: Int,
    val ticket_number: String,
    val department_id: Int,
    val queue_type_id: Int,
    val status: String,
    val service_date: String,
    val queuedAt: String,
    val waitingSeconds: Long?,
    val waitingDisplay: String,
    val servedSeconds: Long?,
    val servedDisplay: String
)

fun TicketCreateRequest.validateTicketCreateRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (kiosk_id <= 0) errors.add(GlobalCredentialResponse(400, false, "kiosk_id is required"))
    if (queue_type_id <= 0) errors.add(GlobalCredentialResponse(400, false, "queue_type_id is required"))
    return errors
}
