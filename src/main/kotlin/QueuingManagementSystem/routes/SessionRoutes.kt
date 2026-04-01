package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.common.Role
import QueuingManagementSystem.common.normalizedRole
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.SessionController
import QueuingManagementSystem.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.sessionRoutes() {
    val authController = AuthController()
    val sessionController = SessionController()

    route("/sessions") {
        get("/me") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!session.permissions.contains("session_view_self")) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }
            call.respond(HttpStatusCode.OK, ListResponse(sessionController.getSessionsForUser(session.userId), GlobalCredentialResponse(200, true, "OK")))
        }

        get("/admin") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!session.permissions.contains("session_view_all")) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }

            val requestedDepartment = call.request.queryParameters["departmentId"]?.toIntOrNull()
            val requestedUser = call.request.queryParameters["userId"]?.toIntOrNull()
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 100).coerceIn(1, 500)
            val offset = (call.request.queryParameters["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)

            if (session.normalizedRole() != Role.SUPER_ADMIN) {
                if (requestedDepartment != null && requestedDepartment != session.departmentId) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }
                val scopedDepartmentId = session.departmentId
                call.respond(HttpStatusCode.OK, ListResponse(sessionController.getSessionsForAdmin(requestedUser, scopedDepartmentId, limit, offset), GlobalCredentialResponse(200, true, "OK")))
            } else {
                call.respond(HttpStatusCode.OK, ListResponse(sessionController.getSessionsForAdmin(requestedUser, requestedDepartment, limit, offset), GlobalCredentialResponse(200, true, "OK")))
            }
        }

        post("/me/revoke-others") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!session.permissions.contains("session_revoke_self_other")) {
                return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }
            val count = sessionController.revokeOwnOtherSessions(session.userId, session.departmentId, session.sessionId)
            call.respond(HttpStatusCode.OK, mapOf("revoked_count" to count, "result" to GlobalCredentialResponse(200, true, "Other sessions revoked")))
        }

        post("/{sessionId}/revoke") {
            val actor = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!actor.permissions.contains("session_revoke_any")) {
                return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }

            val sessionId = call.parameters["sessionId"] ?: ""
            if (sessionId.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "sessionId is required"))

            if (actor.normalizedRole() != Role.SUPER_ADMIN && !sessionController.canAdminAccessSession(sessionId, actor.departmentId)) {
                return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
            }

            val reason = call.request.queryParameters["reason"] ?: "ADMIN_FORCED_LOGOUT"
            val ok = sessionController.revokeAnySession(actor.userId, actor.departmentId, sessionId, reason)
            call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, ok, if (ok) "Session revoked" else "Session not active"))
        }
    }
}
