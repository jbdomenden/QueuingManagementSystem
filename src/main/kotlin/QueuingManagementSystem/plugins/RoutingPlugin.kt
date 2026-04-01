package QueuingManagementSystem.plugins

import QueuingManagementSystem.routes.areaRoutes
import QueuingManagementSystem.routes.auditRoutes
import QueuingManagementSystem.routes.authRoutes
import QueuingManagementSystem.routes.departmentRoutes
import QueuingManagementSystem.routes.dashboardRoutes
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
import QueuingManagementSystem.routes.staffAuthRoutes
import QueuingManagementSystem.routes.ticketRoutes
import QueuingManagementSystem.routes.userRoutes
import QueuingManagementSystem.routes.windowRoutes
import QueuingManagementSystem.routes.workflowTemplateRoutes
import QueuingManagementSystem.routes.staffAccessRoutes
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondText
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
            call.respondText(
                contentType = ContentType.Text.Html,
                text = """
                    <!doctype html>
                    <html lang="en">
                    <head>
                      <meta charset="UTF-8" />
                      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                      <title>QMS Quick Links</title>
                    </head>
                    <body style="font-family: Arial, sans-serif; padding: 24px;">
                      <h2>Available quick-access endpoints</h2>
                      <ul>
                        <li><a href="/kiosk.html">Kiosk</a></li>
                        <li><a href="/display.html">Display</a></li>
                        <li><a href="/admin.html">User/Admin</a></li>
                        <li><a href="/index.html">Login</a></li>
                      </ul>
                    </body>
                    </html>
                """.trimIndent()
            )
        }

        staticResources("", "static")

        authRoutes()
        staffAuthRoutes()
        staffAccessRoutes()
        sessionRoutes()
        dashboardRoutes()
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
