package QueuingManagementSystem.plugins

import QueuingManagementSystem.routes.areaRoutes
import QueuingManagementSystem.routes.auditRoutes
import QueuingManagementSystem.routes.authRoutes
import QueuingManagementSystem.routes.departmentRoutes
import QueuingManagementSystem.routes.companyRoutes
import QueuingManagementSystem.routes.companyTransactionRoutes
import QueuingManagementSystem.routes.crewValidationRoutes
import QueuingManagementSystem.routes.companyTransactionDestinationRoutes
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
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticResources

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondRedirect("/index.html")
        }

        staticResources("/", "static")

        authRoutes()
        departmentRoutes()
        companyRoutes()
        companyTransactionRoutes()
        companyTransactionDestinationRoutes()
        crewValidationRoutes()
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
