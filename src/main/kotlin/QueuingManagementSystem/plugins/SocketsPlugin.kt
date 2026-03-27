package QueuingManagementSystem.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets

fun Application.configureSockets() {
    install(WebSockets) {
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
