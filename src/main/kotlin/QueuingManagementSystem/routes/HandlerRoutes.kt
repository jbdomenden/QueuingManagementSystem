package QueuingManagementSystem.routes

import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.HandlerController
import QueuingManagementSystem.models.*
import QueuingManagementSystem.models.validateHandlerRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.handlerRoutes() {
    val authController = AuthController()
    val controller = HandlerController()

    route("/handlers") {
        post("/create") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                val request = call.receive<HandlerRequest>()
                val errors = request.validateHandlerRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                call.respond(HttpStatusCode.OK, IdResponse(controller.createHandler(request), GlobalCredentialResponse(200, true, "Handler created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update") {
            try {
                val request = call.receive<HandlerRequest>()
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.updateHandler(request), "Handler updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/start-session") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.HANDLER.name, UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<HandlerSessionRequest>()
                val handler = controller.getActiveHandlerByUserId(session.user_id)
                if (session.role == UserRole.HANDLER.name && (handler == null || handler.id != request.handler_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
                }

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
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.HANDLER.name, UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<HandlerSessionRequest>()
                val handler = controller.getActiveHandlerByUserId(session.user_id)
                if (session.role == UserRole.HANDLER.name && (handler == null || handler.id != request.handler_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
                }

                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.endSession(request.handler_id), "Session ended"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list/{departmentId}") {
            try {
                val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0
                call.respond(HttpStatusCode.OK, ListResponse(controller.getHandlersByDepartment(departmentId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
