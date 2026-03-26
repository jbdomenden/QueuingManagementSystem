package QueuingManagementSystem.routes

import QueuingManagementSystem.controllers.AuditController
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.auditRoutes() {
    val controller = AuditController()
    route("/audit") {
        get("/logs") {
            val departmentId = call.request.queryParameters["departmentId"]?.toIntOrNull()
            call.respond(HttpStatusCode.OK, controller.getAuditLogs(departmentId))
        }
    }
}
