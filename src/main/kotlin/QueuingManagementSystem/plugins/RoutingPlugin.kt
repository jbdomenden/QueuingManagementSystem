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
import QueuingManagementSystem.routes.sessionRoutes
import QueuingManagementSystem.routes.ticketRoutes
import QueuingManagementSystem.routes.userRoutes
import QueuingManagementSystem.routes.windowRoutes
import QueuingManagementSystem.routes.workflowTemplateRoutes
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(
                mapOf(
                    "message" to "Available quick-access endpoints",
                    "endpoints" to mapOf(
                        "kiosk" to "/kiosk.html",
                        "display" to "/display.html",
                        "user" to "/admin.html"
                    )
                )
            )
        }

        staticResources("", "static")

        authRoutes()
        sessionRoutes()
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
        workflowTemplateRoutes()
        reportRoutes()
        auditRoutes()
        realtimeRoutes()
    }
}
