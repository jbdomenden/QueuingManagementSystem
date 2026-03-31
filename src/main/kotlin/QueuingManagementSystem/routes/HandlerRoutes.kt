package QueuingManagementSystem.routes

import QueuingManagementSystem.common.canAccessDepartment
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.AuditController
import QueuingManagementSystem.controllers.HandlerController
import QueuingManagementSystem.models.*
import QueuingManagementSystem.models.validateHandlerRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.handlerRoutes() {
    val authController = AuthController()
    val auditController = AuditController()
    val controller = HandlerController()

    route("/handlers") {
        post("/create") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("handler_manage")) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                val request = call.receive<HandlerRequest>()
                val errors = request.validateHandlerRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                if (!session.canAccessDepartment(request.department_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                val createdId = controller.createHandler(request)
                if (createdId > 0) {
                    auditController.createAuditLog(session.userId, request.department_id, "ADMIN_SCOPE_HANDLER_CREATE", "handlers", createdId.toString(), "{\"department_id\":${request.department_id}}")
                }
                call.respond(HttpStatusCode.OK, IdResponse(createdId, GlobalCredentialResponse(200, true, "Handler created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("handler_manage")) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                val request = call.receive<HandlerRequest>()

                val existing = request.id?.let { controller.getHandlerById(it) }
                    ?: return@put call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Handler not found"))
                if (!session.canAccessDepartment(existing.department_id)) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))

                val updated = controller.updateHandler(request)
                if (updated && request.id != null) {
                    auditController.createAuditLog(session.userId, existing.department_id, "ADMIN_SCOPE_HANDLER_UPDATE", "handlers", request.id.toString(), "{\"department_id\":${existing.department_id}}")
                }
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, updated, "Handler updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/start-session") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

                val request = call.receive<HandlerSessionRequest>()
                val targetHandler = controller.getHandlerById(request.handler_id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Handler not found"))

                val canManage = session.permissions.contains("handler_manage") && session.canAccessDepartment(targetHandler.department_id)
                val selfHandler = controller.getActiveHandlerByUserId(session.userId)
                val isSelfHandlerAction = selfHandler?.id == request.handler_id
                if (!canManage && !isSelfHandlerAction) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))

                val started = controller.startSession(request.handler_id, request.window_id)
                if (!started) return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Unable to start session"))
                val activeSession = controller.getActiveSession(request.handler_id)
                call.respond(HttpStatusCode.OK, activeSession ?: GlobalCredentialResponse(200, true, "Session started"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/end-session") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

                val request = call.receive<HandlerSessionRequest>()
                val targetHandler = controller.getHandlerById(request.handler_id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Handler not found"))

                val canManage = session.permissions.contains("handler_manage") && session.canAccessDepartment(targetHandler.department_id)
                val selfHandler = controller.getActiveHandlerByUserId(session.userId)
                val isSelfHandlerAction = selfHandler?.id == request.handler_id
                if (!canManage && !isSelfHandlerAction) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))

                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.endSession(request.handler_id), "Session ended"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list/{departmentId}") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("handler_manage")) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0
                if (departmentId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
                if (!session.canAccessDepartment(departmentId)) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                call.respond(HttpStatusCode.OK, ListResponse(controller.getHandlersByDepartment(departmentId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
