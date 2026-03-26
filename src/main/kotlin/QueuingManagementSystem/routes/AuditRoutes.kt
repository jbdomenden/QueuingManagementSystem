package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import QueuingManagementSystem.controllers.AuditController

fun Route.auditRoutes() {
    val controller = _root_ide_package_.QueuingManagementSystem.controllers.AuditController()
    route("/audit") {
        get("/logs") {
            val departmentId = call.request.queryParameters["departmentId"]?.toIntOrNull()
            call.respond(HttpStatusCode.OK, controller.getAuditLogs(departmentId))
        }
    }
}
