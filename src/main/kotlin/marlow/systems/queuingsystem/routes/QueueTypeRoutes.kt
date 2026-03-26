package marlow.systems.queuingsystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import marlow.systems.queuingsystem.common.UserRole
import marlow.systems.queuingsystem.common.extractBearerToken
import marlow.systems.queuingsystem.controllers.AuthController
import marlow.systems.queuingsystem.controllers.QueueTypeController
import marlow.systems.queuingsystem.models.*

fun Route.queueTypeRoutes() {
    val authController = AuthController()
    val controller = QueueTypeController()
    route("/queue-types") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val request = call.receive<QueueTypeRequest>(); val errors = request.validateQueueTypeRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation")); call.respond(HttpStatusCode.OK, IdResponse(controller.createQueueType(request), GlobalCredentialResponse(200, true, "Queue type created"))) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        put("/update") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val request = call.receive<QueueTypeRequest>(); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation")); call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.updateQueueType(request), "Queue type updated")) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        get("/list/{departmentId}") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0; if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != departmentId) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation")); call.respond(HttpStatusCode.OK, ListResponse(controller.getQueueTypesByDepartment(departmentId), GlobalCredentialResponse(200, true, "OK"))) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
    }
}
