package QueuingManagementSystem.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object DisplaySocketManager {
    private val sessions = mutableMapOf<Int, MutableList<WebSocketSession>>()

    fun connect(displayBoardId: Int, session: WebSocketSession) {
        sessions.getOrPut(displayBoardId) { mutableListOf() }.add(session)
    }

    fun disconnect(displayBoardId: Int, session: WebSocketSession) {
        sessions[displayBoardId]?.remove(session)
        if (sessions[displayBoardId].isNullOrEmpty()) {
            sessions.remove(displayBoardId)
        }
    }

    suspend fun publishDisplayUpdate(displayBoardId: Int, event: String, payload: JsonElement) {
        val message = Json.encodeToString(
            buildJsonObject {
                put("event", event)
                put("payload", payload)
            }
        )
        sessions[displayBoardId]?.forEach { socket ->
            socket.send(Frame.Text(message))
        }
    }
}
