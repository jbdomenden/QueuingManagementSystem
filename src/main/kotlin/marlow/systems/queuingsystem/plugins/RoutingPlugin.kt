package marlow.systems.queuingsystem.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import marlow.systems.queuingsystem.routes.*

fun Application.configureRouting() {
    routing {
        authRoutes()
        departmentRoutes()
        userRoutes()
        areaRoutes()
        windowRoutes()
        handlerRoutes()
        queueTypeRoutes()
        kioskRoutes()
        displayRoutes()
        ticketRoutes()
        reportRoutes()
        auditRoutes()
        realtimeRoutes()
    }
}
