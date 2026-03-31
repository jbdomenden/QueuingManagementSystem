package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.DisplayController
import QueuingManagementSystem.models.DisplayFilterParams
import QueuingManagementSystem.realtime.AdminSocketManager
import QueuingManagementSystem.realtime.DisplaySocketManager
import QueuingManagementSystem.realtime.HandlerSocketManager
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.realtimeRoutes() {
    val authController = AuthController()
    val displayController = DisplayController()

    route("/realtime") {
        webSocket("/ws/handler/{handlerId}") {
            val handlerId = call.parameters["handlerId"]?.toIntOrNull() ?: 0
            val windowId = call.request.queryParameters["windowId"]?.toIntOrNull() ?: 0
            if (handlerId <= 0 || windowId <= 0) {
                close()
                return@webSocket
            }

            HandlerSocketManager.connect(handlerId, windowId, this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text && frame.readText() == "ping") outgoing.send(Frame.Text("pong"))
                }
            } finally {
                HandlerSocketManager.disconnect(handlerId, windowId)
            }
        }

        webSocket("/ws/display/{displayId}") {
            val displayId = call.parameters["displayId"]?.toIntOrNull() ?: 0
            if (displayId <= 0) {
                close()
                return@webSocket
            }
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
            if (session == null || !session.permissions.contains("display_view")) {
                close()
                return@webSocket
            }
            if (!session.permissions.contains("display_scope_department") && !session.permissions.contains("display_scope_global")) {
                close()
                return@webSocket
            }
            val display = displayController.getDisplayBoardById(displayId)
            if (display == null) {
                close()
                return@webSocket
            }
            if (session.permissions.contains("display_scope_department") && session.departmentId != display.department_id) {
                close()
                return@webSocket
            }

            val filters = DisplayFilterParams(
                department_id = call.request.queryParameters["department_id"]?.toIntOrNull(),
                area_id = call.request.queryParameters["area_id"]?.toIntOrNull(),
                floor_id = call.request.queryParameters["floor_id"]?.toIntOrNull(),
                company_id = call.request.queryParameters["company_id"]?.toIntOrNull()
            )
            val filtersJson = Json.encodeToString(filters)

            DisplaySocketManager.connect(displayId, this, filtersJson)
            val initialPayload = Json.encodeToString(displayController.getDisplayAggregateSnapshot(displayId, filters))
            outgoing.send(Frame.Text("{\"event\":\"DISPLAY_CONNECTED\",\"payload\":$initialPayload}"))
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text && frame.readText() == "ping") outgoing.send(Frame.Text("pong"))
                }
            } finally {
                DisplaySocketManager.disconnect(displayId, this)
            }
        }

        webSocket("/ws/admin") {
            AdminSocketManager.connect(this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text && frame.readText() == "ping") outgoing.send(Frame.Text("pong"))
                }
            } finally {
                AdminSocketManager.disconnect(this)
            }
        }
    }
}
