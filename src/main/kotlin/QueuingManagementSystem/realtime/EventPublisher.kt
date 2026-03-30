package QueuingManagementSystem.realtime

import QueuingManagementSystem.controllers.DisplayController
import QueuingManagementSystem.controllers.HandlerController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.parseToJsonElement

class EventPublisher {
    private val handlerController = HandlerController()
    private val displayController = DisplayController()

    suspend fun notifyHandlersTicketCreated(queueTypeId: Int) {
        val payload = buildJsonObject {
            put("queue_type_id", queueTypeId)
        }
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
        AdminSocketManager.publishSummary(
            "DEPARTMENT_SUMMARY_UPDATED",
            buildJsonObject { put("department_id", departmentId) }
        )
    }

    suspend fun publishTicketCreated(queueTypeId: Int, departmentDisplayIds: List<Int>) {
        notifyHandlersTicketCreated(queueTypeId)
        notifyDisplayTicketCreated(departmentDisplayIds)
    }

    suspend fun publishTicketCalled(displayIds: List<Int>, ticketId: Int) {
        notifyDisplayTicketCalled(displayIds)
        AdminSocketManager.publishSummary("TICKET_CALLED", buildJsonObject { put("ticket_id", ticketId) })
    }

    suspend fun publishTicketSkipped(displayIds: List<Int>, ticketId: Int) {
        notifyDisplayTicketSkipped(displayIds)
        AdminSocketManager.publishSummary("TICKET_SKIPPED", buildJsonObject { put("ticket_id", ticketId) })
    }

    suspend fun publishTicketCompleted(displayIds: List<Int>, ticketId: Int) {
        notifyDisplayTicketCompleted(displayIds)
        AdminSocketManager.publishSummary("TICKET_COMPLETED", buildJsonObject { put("ticket_id", ticketId) })
    }

    suspend fun publishDepartmentSummaryUpdate(departmentId: Int) {
        notifyAdminDepartmentSummary(departmentId)
    }

    private suspend fun publishDisplaySnapshots(displayIds: List<Int>, event: String) {
        displayIds.distinct().forEach { displayId ->
            val snapshot = displayController.getDisplaySnapshot(displayId)
            val payload = Json.parseToJsonElement(Json.encodeToString(snapshot))
            DisplaySocketManager.publishDisplayUpdate(displayId, event, payload)
        }
    }
}
