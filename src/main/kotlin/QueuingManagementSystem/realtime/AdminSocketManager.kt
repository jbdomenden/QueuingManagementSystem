package QueuingManagementSystem.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AdminSocketManager {
    private val sessions = mutableListOf<WebSocketSession>()

    fun connect(session: WebSocketSession) {
        sessions.add(session)
    }

    fun disconnect(session: WebSocketSession) {
        sessions.remove(session)
    }

    suspend fun publishSummary(event: String, message: String) {
        val payload = Json.encodeToString(buildJsonObject {
            put("event", event)
            put("message", message)
        })
        sessions.forEach { socket -> socket.send(Frame.Text(payload)) }
    }
}
