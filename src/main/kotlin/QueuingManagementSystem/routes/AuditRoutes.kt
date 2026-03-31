package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.AuditController
import QueuingManagementSystem.models.GlobalCredentialResponse

fun Route.auditRoutes() {
    val authController = AuthController()
    val controller = QueuingManagementSystem.controllers.AuditController()
    route("/audit") {
        get("/logs") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!session.permissions.contains("audit_view")) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }
            val departmentId = call.request.queryParameters["departmentId"]?.toIntOrNull()
            if (session.permissions.contains("report_view_department") && !session.permissions.contains("report_view_global") && departmentId != null && !session.departmentScopes.contains(departmentId)) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
            }
            call.respond(HttpStatusCode.OK, controller.getAuditLogs(departmentId))
        }
    }
}
