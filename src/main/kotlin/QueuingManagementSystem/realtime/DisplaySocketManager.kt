package QueuingManagementSystem.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession

object DisplaySocketManager {
    private val sessions = mutableMapOf<Int, MutableList<WebSocketSession>>()

    fun connect(displayBoardId: Int, session: WebSocketSession) {
        sessions.getOrPut(displayBoardId) { mutableListOf() }.add(session)
    }

    fun disconnect(displayBoardId: Int, session: WebSocketSession) {
        sessions[displayBoardId]?.remove(session)
        if (sessions[displayBoardId].isNullOrEmpty()) sessions.remove(displayBoardId)
    }

    suspend fun publishDisplayUpdate(displayBoardId: Int, event: String, payload: String) {
        sessions[displayBoardId]?.forEach { socket ->
            socket.send(Frame.Text("{\"event\":\"$event\",\"payload\":$payload}"))
        }
    }
}
