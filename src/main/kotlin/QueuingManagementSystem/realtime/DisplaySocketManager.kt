package QueuingManagementSystem.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object DisplaySocketManager {
    data class DisplaySubscription(
        val session: WebSocketSession,
        val filtersJson: String
    )

    private val sessions = mutableMapOf<Int, MutableList<DisplaySubscription>>()

    fun connect(displayBoardId: Int, session: WebSocketSession, filtersJson: String) {
        sessions.getOrPut(displayBoardId) { mutableListOf() }.add(DisplaySubscription(session, filtersJson))
    }

    fun disconnect(displayBoardId: Int, session: WebSocketSession) {
        sessions[displayBoardId]?.removeIf { it.session == session }
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
        sessions[displayBoardId]?.forEach { subscription ->
            subscription.session.send(Frame.Text(message))
        }
    }

    fun getSubscriptions(displayBoardId: Int): List<DisplaySubscription> {
        return sessions[displayBoardId]?.toList() ?: emptyList()
    }
}
