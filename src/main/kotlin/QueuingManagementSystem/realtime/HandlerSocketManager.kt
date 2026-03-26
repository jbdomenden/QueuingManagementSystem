package QueuingManagementSystem.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession

object HandlerSocketManager {
    private val sessions = mutableMapOf<Int, MutableMap<Int, WebSocketSession>>()

    fun connect(handlerId: Int, windowId: Int, session: WebSocketSession) {
        sessions.getOrPut(handlerId) { mutableMapOf() }[windowId] = session
    }

    fun disconnect(handlerId: Int, windowId: Int) {
        sessions[handlerId]?.remove(windowId)
        if (sessions[handlerId].isNullOrEmpty()) sessions.remove(handlerId)
    }

    suspend fun notifyHandler(handlerId: Int, event: String, payload: String) {
        sessions[handlerId]?.values?.forEach { socket ->
            socket.send(Frame.Text("{\"event\":\"$event\",\"payload\":$payload}"))
        }
    }
}
