package QueuingManagementSystem.realtime

import QueuingManagementSystem.controllers.DisplayController
import QueuingManagementSystem.controllers.HandlerController
import kotlinx.serialization.json.Json

class EventPublisher {
    private val handlerController = HandlerController()
    private val displayController = DisplayController()

    suspend fun publishNewTicket(queueTypeId: Int, departmentDisplayIds: List<Int>) {
        val handlerIds = handlerController.getActiveHandlersForQueueType(queueTypeId)
        handlerIds.forEach { handlerId ->
            HandlerSocketManager.notifyHandler(handlerId, "NEW_TICKET_AVAILABLE", "{\"queue_type_id\":$queueTypeId}")
        }

        departmentDisplayIds.forEach { displayId ->
            val snapshot = displayController.getDisplaySnapshot(displayId)
            DisplaySocketManager.publishDisplayUpdate(displayId, Json.encodeToString(snapshot))
        }

        AdminSocketManager.publishSummary("QUEUE_CHANGED", "Ticket created for queue_type_id=$queueTypeId")
    }
}
