package QueuingManagementSystem.routes

import QueuingManagementSystem.realtime.AdminSocketManager
import QueuingManagementSystem.realtime.DisplaySocketManager
import QueuingManagementSystem.realtime.HandlerSocketManager
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

fun Route.realtimeRoutes() {
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

            DisplaySocketManager.connect(displayId, this)
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
