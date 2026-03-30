package QueuingManagementSystem.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object HandlerSocketManager {
    private val sessions = mutableMapOf<Int, MutableMap<Int, WebSocketSession>>()

    fun connect(handlerId: Int, windowId: Int, session: WebSocketSession) {
        sessions.getOrPut(handlerId) { mutableMapOf() }[windowId] = session
    }

    fun disconnect(handlerId: Int, windowId: Int) {
        sessions[handlerId]?.remove(windowId)
        if (sessions[handlerId].isNullOrEmpty()) {
            sessions.remove(handlerId)
        }
    }

    suspend fun notifyHandler(handlerId: Int, event: String, payload: JsonObject) {
        val message = Json.encodeToString(
            buildJsonObject {
                put("event", event)
                put("payload", payload)
            }
        )
        sessions[handlerId]?.values?.forEach { socket ->
            socket.send(Frame.Text(message))
        }
    }

    suspend fun notifyHandler(handlerId: Int, event: String, payload: String) {
        notifyHandler(
            handlerId,
            event,
            buildJsonObject {
                put("raw", JsonPrimitive(payload))
            }
        )
    }
}
