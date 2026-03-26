package QueuingManagementSystem.realtime

import QueuingManagementSystem.controllers.DisplayController
import QueuingManagementSystem.controllers.HandlerController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EventPublisher {
    private val handlerController = HandlerController()
    private val displayController = DisplayController()

    suspend fun publishTicketCreated(queueTypeId: Int, departmentDisplayIds: List<Int>) {
        val payload = "{\"queue_type_id\":$queueTypeId}"
        val handlerIds = handlerController.getActiveHandlersForQueueType(queueTypeId)
        handlerIds.forEach { handlerId ->
            HandlerSocketManager.notifyHandler(handlerId, "TICKET_CREATED", payload)
        }

        publishDisplaySnapshots(departmentDisplayIds)
        AdminSocketManager.publishSummary("TICKET_CREATED", "Ticket created for queue_type_id=$queueTypeId")
    }

    suspend fun publishTicketCalled(displayIds: List<Int>, ticketId: Int) {
        publishDisplaySnapshots(displayIds)
        AdminSocketManager.publishSummary("TICKET_CALLED", "Ticket called, ticket_id=$ticketId")
    }

    suspend fun publishTicketSkipped(displayIds: List<Int>, ticketId: Int) {
        publishDisplaySnapshots(displayIds)
        AdminSocketManager.publishSummary("TICKET_SKIPPED", "Ticket skipped, ticket_id=$ticketId")
    }

    suspend fun publishTicketCompleted(displayIds: List<Int>, ticketId: Int) {
        publishDisplaySnapshots(displayIds)
        AdminSocketManager.publishSummary("TICKET_COMPLETED", "Ticket completed, ticket_id=$ticketId")
    }

    suspend fun publishDepartmentSummaryUpdate(departmentId: Int) {
        AdminSocketManager.publishSummary("DEPARTMENT_SUMMARY_UPDATED", "department_id=$departmentId")
    }

    private suspend fun publishDisplaySnapshots(displayIds: List<Int>) {
        displayIds.distinct().forEach { displayId ->
            val snapshot = displayController.getDisplaySnapshot(displayId)
            DisplaySocketManager.publishDisplayUpdate(displayId, Json.encodeToString(snapshot))
        }
    }
}
