package QueuingManagementSystem.routes

import QueuingManagementSystem.realtime.AdminSocketManager
import QueuingManagementSystem.realtime.DisplaySocketManager
import QueuingManagementSystem.realtime.HandlerSocketManager
import io.ktor.server.routing.Route
import io.ktor.server.routing.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlin.text.get

fun Route.realtimeRoutes() {
    webSocket("/ws/handler/{handlerId}") {
        val handlerId = call.parameters["handlerId"]?.toIntOrNull() ?: 0
        HandlerSocketManager.connect(handlerId, this)
        try { for (frame in incoming) { if (frame is Frame.Text && frame.readText() == "ping") outgoing.send(Frame.Text("pong")) } } finally { HandlerSocketManager.disconnect(handlerId) }
    }

    webSocket("/ws/display/{displayId}") {
        val displayId = call.parameters["displayId"]?.toIntOrNull() ?: 0
        DisplaySocketManager.connect(displayId, this)
        try { for (frame in incoming) { if (frame is Frame.Text && frame.readText() == "ping") outgoing.send(Frame.Text("pong")) } } finally { DisplaySocketManager.disconnect(displayId, this) }
    }

    webSocket("/ws/admin") {
        AdminSocketManager.connect(this)
        try { for (frame in incoming) { if (frame is Frame.Text && frame.readText() == "ping") outgoing.send(Frame.Text("pong")) } } finally { AdminSocketManager.disconnect(this) }
    }
}
