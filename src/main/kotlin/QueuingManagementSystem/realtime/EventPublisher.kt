package QueuingManagementSystem.realtime

import kotlinx.serialization.json.Json
import QueuingManagementSystem.controllers.DisplayController
import QueuingManagementSystem.controllers.HandlerController

class EventPublisher {
    private val handlerController = _root_ide_package_.QueuingManagementSystem.controllers.HandlerController()
    private val displayController = _root_ide_package_.QueuingManagementSystem.controllers.DisplayController()

    suspend fun publishNewTicket(queueTypeId: Int, departmentDisplayIds: List<Int>) {
        val handlerIds = handlerController.getActiveHandlersForQueueType(queueTypeId)
        handlerIds.forEach { handlerId ->
            _root_ide_package_.QueuingManagementSystem.realtime.HandlerSocketManager.notifyHandler(handlerId, "NEW_TICKET_AVAILABLE", "{\"queue_type_id\":$queueTypeId}")
        }

        departmentDisplayIds.forEach { displayId ->
            val snapshot = displayController.getDisplaySnapshot(displayId)
            _root_ide_package_.QueuingManagementSystem.realtime.DisplaySocketManager.publishDisplayUpdate(displayId, Json.encodeToString(snapshot))
        }

        _root_ide_package_.QueuingManagementSystem.realtime.AdminSocketManager.publishSummary("QUEUE_CHANGED", "Ticket created for queue_type_id=$queueTypeId")
    }
}
