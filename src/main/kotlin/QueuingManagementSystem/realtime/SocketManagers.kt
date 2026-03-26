package QueuingManagementSystem.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object HandlerSocketManager {
    private val sessions = mutableMapOf<Int, WebSocketSession>()
    fun connect(handlerId: Int, session: WebSocketSession) { sessions[handlerId] = session }
    fun disconnect(handlerId: Int) { sessions.remove(handlerId) }
    suspend fun notifyHandler(handlerId: Int, event: String, payload: String) {
        sessions[handlerId]?.send(Frame.Text("{\"event\":\"$event\",\"payload\":$payload}"))
    }
}

object DisplaySocketManager {
    private val sessions = mutableMapOf<Int, MutableList<WebSocketSession>>()
    fun connect(displayBoardId: Int, session: WebSocketSession) { sessions.getOrPut(displayBoardId) { mutableListOf() }.add(session) }
    fun disconnect(displayBoardId: Int, session: WebSocketSession) { sessions[displayBoardId]?.remove(session) }
    suspend fun publishDisplayUpdate(displayBoardId: Int, payload: String) {
        sessions[displayBoardId]?.forEach { it.send(Frame.Text(payload)) }
    }
}

object AdminSocketManager {
    private val sessions = mutableListOf<WebSocketSession>()
    fun connect(session: WebSocketSession) { sessions.add(session) }
    fun disconnect(session: WebSocketSession) { sessions.remove(session) }
    suspend fun publishSummary(event: String, message: String) {
        val payload = Json.encodeToString(buildJsonObject { put("event", event); put("message", message) })
        sessions.forEach { it.send(Frame.Text(payload)) }
    }
}
