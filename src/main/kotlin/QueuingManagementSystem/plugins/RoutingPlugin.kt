package QueuingManagementSystem.plugins

import QueuingManagementSystem.routes.areaRoutes
import QueuingManagementSystem.routes.auditRoutes
import QueuingManagementSystem.routes.authRoutes
import QueuingManagementSystem.routes.departmentRoutes
import QueuingManagementSystem.routes.displayRoutes
import QueuingManagementSystem.routes.handlerRoutes
import QueuingManagementSystem.routes.kioskRoutes
import QueuingManagementSystem.routes.queueTypeRoutes
import QueuingManagementSystem.routes.realtimeRoutes
import QueuingManagementSystem.routes.reportRoutes
import QueuingManagementSystem.routes.ticketRoutes
import QueuingManagementSystem.routes.userRoutes
import QueuingManagementSystem.routes.windowRoutes
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondRedirect("/kiosk.html")
        }

        staticResources("", "static")

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
