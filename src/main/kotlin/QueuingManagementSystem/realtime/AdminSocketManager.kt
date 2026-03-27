package QueuingManagementSystem.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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

    suspend fun publishSummary(event: String, payload: JsonObject) {
        val message = Json.encodeToString(
            buildJsonObject {
                put("event", event)
                put("payload", payload)
            }
        )
        sessions.forEach { socket -> socket.send(Frame.Text(message)) }
    }

    suspend fun publishSummary(event: String, message: String) {
        publishSummary(
            event,
            buildJsonObject {
                put("message", message)
            }
        )
    }
}
