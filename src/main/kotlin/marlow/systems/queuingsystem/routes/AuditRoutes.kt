package marlow.systems.queuingsystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import marlow.systems.queuingsystem.controllers.AuditController

fun Route.auditRoutes() {
    val controller = AuditController()
    route("/audit") {
        get("/logs") {
            val departmentId = call.request.queryParameters["departmentId"]?.toIntOrNull()
            call.respond(HttpStatusCode.OK, controller.getAuditLogs(departmentId))
        }
    }
}
