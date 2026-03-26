package marlow.systems.queuingsystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import marlow.systems.queuingsystem.common.UserRole
import marlow.systems.queuingsystem.common.extractBearerToken
import marlow.systems.queuingsystem.controllers.AuthController
import marlow.systems.queuingsystem.controllers.HandlerController
import marlow.systems.queuingsystem.models.*

fun Route.handlerRoutes() {
    val authController = AuthController()
    val controller = HandlerController()
    route("/handlers") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden")); val request = call.receive<HandlerRequest>(); val errors = request.validateHandlerRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation")); call.respond(HttpStatusCode.OK, IdResponse(controller.createHandler(request), GlobalCredentialResponse(200, true, "Handler created"))) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        put("/update") { try { val request = call.receive<HandlerRequest>(); call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.updateHandler(request), "Handler updated")) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        post("/start-session") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(UserRole.HANDLER.name, UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden")); val request = call.receive<HandlerSessionRequest>(); call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.startSession(request.handler_id, request.window_id), "Session started")) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        post("/end-session") { try { val request = call.receive<HandlerSessionRequest>(); call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.endSession(request.handler_id), "Session ended")) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        get("/list/{departmentId}") { try { val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0; call.respond(HttpStatusCode.OK, ListResponse(controller.getHandlersByDepartment(departmentId), GlobalCredentialResponse(200, true, "OK"))) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
    }
}
