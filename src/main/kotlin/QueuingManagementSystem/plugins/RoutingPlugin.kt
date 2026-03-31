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
import kotlinx.serialization.Serializable

@Serializable
data class RootIndexResponse(
    val message: String,
    val endpoints: Map<String, String>
)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(
                RootIndexResponse(
                    message = "Available quick-access endpoints",
                    endpoints = mapOf(
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
