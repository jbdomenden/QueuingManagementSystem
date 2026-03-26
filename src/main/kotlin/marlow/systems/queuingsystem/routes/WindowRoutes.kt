package marlow.systems.queuingsystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import marlow.systems.queuingsystem.common.UserRole
import marlow.systems.queuingsystem.common.extractBearerToken
import marlow.systems.queuingsystem.controllers.AuthController
import marlow.systems.queuingsystem.controllers.WindowController
import marlow.systems.queuingsystem.models.*

fun Route.windowRoutes() {
    val authController = AuthController()
    val controller = WindowController()
    route("/windows") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden")); val request = call.receive<WindowRequest>(); val errors = request.validateWindowRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation")); call.respond(HttpStatusCode.OK, IdResponse(controller.createWindow(request), GlobalCredentialResponse(200, true, "Window created"))) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        put("/update") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val request = call.receive<WindowRequest>(); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation")); call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.updateWindow(request), "Window updated")) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        get("/list/{departmentId}") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0; if (departmentId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required")); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != departmentId) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation")); call.respond(HttpStatusCode.OK, ListResponse(controller.getWindowsByDepartment(departmentId), GlobalCredentialResponse(200, true, "OK"))) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        post("/assign-queue-types") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden")); val request = call.receive<WindowQueueTypeAssignmentRequest>(); call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.assignQueueTypes(request), "Window queue types updated")) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
    }
}
