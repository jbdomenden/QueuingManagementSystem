package QueuingManagementSystem.realtime

import QueuingManagementSystem.controllers.DisplayController
import QueuingManagementSystem.controllers.HandlerController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EventPublisher {
    private val handlerController = HandlerController()
    private val displayController = DisplayController()

    suspend fun notifyHandlersTicketCreated(queueTypeId: Int) {
        val payload = "{\"queue_type_id\":$queueTypeId}"
        val handlerIds = handlerController.getActiveHandlersForQueueType(queueTypeId)
        handlerIds.forEach { handlerId ->
            HandlerSocketManager.notifyHandler(handlerId, "TICKET_CREATED", payload)
        }
    }

    suspend fun notifyDisplayTicketCreated(displayIds: List<Int>) {
        publishDisplaySnapshots(displayIds, "DISPLAY_TICKET_CREATED")
    }

    suspend fun notifyDisplayTicketCalled(displayIds: List<Int>) {
        publishDisplaySnapshots(displayIds, "DISPLAY_TICKET_CALLED")
    }

    suspend fun notifyDisplayTicketSkipped(displayIds: List<Int>) {
        publishDisplaySnapshots(displayIds, "DISPLAY_TICKET_SKIPPED")
    }

    suspend fun notifyDisplayTicketRecalled(displayIds: List<Int>) {
        publishDisplaySnapshots(displayIds, "DISPLAY_TICKET_RECALLED")
    }

    suspend fun notifyDisplayTicketCompleted(displayIds: List<Int>) {
        publishDisplaySnapshots(displayIds, "DISPLAY_TICKET_COMPLETED")
    }

    suspend fun notifyAdminDepartmentSummary(departmentId: Int) {
        AdminSocketManager.publishSummary("DEPARTMENT_SUMMARY_UPDATED", "department_id=$departmentId")
    }

    suspend fun publishTicketCreated(queueTypeId: Int, departmentDisplayIds: List<Int>) {
        notifyHandlersTicketCreated(queueTypeId)
        notifyDisplayTicketCreated(departmentDisplayIds)
    }

    suspend fun publishTicketCalled(displayIds: List<Int>, ticketId: Int) {
        notifyDisplayTicketCalled(displayIds)
        AdminSocketManager.publishSummary("TICKET_CALLED", "ticket_id=$ticketId")
    }

    suspend fun publishTicketSkipped(displayIds: List<Int>, ticketId: Int) {
        notifyDisplayTicketSkipped(displayIds)
        AdminSocketManager.publishSummary("TICKET_SKIPPED", "ticket_id=$ticketId")
    }

    suspend fun publishTicketCompleted(displayIds: List<Int>, ticketId: Int) {
        notifyDisplayTicketCompleted(displayIds)
        AdminSocketManager.publishSummary("TICKET_COMPLETED", "ticket_id=$ticketId")
    }

    suspend fun publishDepartmentSummaryUpdate(departmentId: Int) {
        notifyAdminDepartmentSummary(departmentId)
    }

    private suspend fun publishDisplaySnapshots(displayIds: List<Int>, event: String) {
        displayIds.distinct().forEach { displayId ->
            val snapshot = displayController.getDisplaySnapshot(displayId)
            DisplaySocketManager.publishDisplayUpdate(displayId, event, Json.encodeToString(snapshot))
        }
    }
}
