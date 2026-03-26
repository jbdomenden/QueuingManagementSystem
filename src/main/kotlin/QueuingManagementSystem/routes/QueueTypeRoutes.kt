package QueuingManagementSystem.routes

import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.QueueTypeController
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.IdResponse
import QueuingManagementSystem.models.ListResponse
import QueuingManagementSystem.models.QueueTypeRequest
import QueuingManagementSystem.models.validateQueueTypeRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.queueTypeRoutes() {
    val authController = AuthController()
    val controller = QueueTypeController()
    route("/queue-types") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val request = call.receive<QueueTypeRequest>(); val errors = request.validateQueueTypeRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden,
            GlobalCredentialResponse(403, false, "Department scope violation")
        ); call.respond(HttpStatusCode.OK,
            IdResponse(controller.createQueueType(request), GlobalCredentialResponse(200, true, "Queue type created"))
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        put("/update") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val request = call.receive<QueueTypeRequest>(); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@put call.respond(HttpStatusCode.Forbidden,
            GlobalCredentialResponse(403, false, "Department scope violation")
        ); call.respond(HttpStatusCode.OK,
            GlobalCredentialResponse(200, controller.updateQueueType(request), "Queue type updated")
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        get("/list/{departmentId}") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0; if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != departmentId) return@get call.respond(HttpStatusCode.Forbidden,
            GlobalCredentialResponse(403, false, "Department scope violation")
        ); call.respond(HttpStatusCode.OK,
            ListResponse(controller.getQueueTypesByDepartment(departmentId), GlobalCredentialResponse(200, true, "OK"))
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
    }
}
