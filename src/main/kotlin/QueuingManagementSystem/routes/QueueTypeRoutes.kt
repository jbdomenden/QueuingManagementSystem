package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.QueueTypeController
import QueuingManagementSystem.models.validateQueueTypeRequest
import marlow.systems.queuingsystem.models.*

fun Route.queueTypeRoutes() {
    val authController = _root_ide_package_.QueuingManagementSystem.controllers.AuthController()
    val controller = _root_ide_package_.QueuingManagementSystem.controllers.QueueTypeController()
    route("/queue-types") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val request = call.receive<QueuingManagementSystem.models.QueueTypeRequest>(); val errors = request.validateQueueTypeRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == _root_ide_package_.QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                403,
                false,
                "Department scope violation"
            )
        ); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.IdResponse(
                controller.createQueueType(request),
                _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                    200,
                    true,
                    "Queue type created"
                )
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        put("/update") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val request = call.receive<QueuingManagementSystem.models.QueueTypeRequest>(); if (session.role == _root_ide_package_.QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@put call.respond(HttpStatusCode.Forbidden,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                403,
                false,
                "Department scope violation"
            )
        ); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                200,
                controller.updateQueueType(request),
                "Queue type updated"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        get("/list/{departmentId}") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0; if (session.role == _root_ide_package_.QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name && session.department_id != departmentId) return@get call.respond(HttpStatusCode.Forbidden,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                403,
                false,
                "Department scope violation"
            )
        ); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.ListResponse(
                controller.getQueueTypesByDepartment(departmentId),
                _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(200, true, "OK")
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
    }
}
