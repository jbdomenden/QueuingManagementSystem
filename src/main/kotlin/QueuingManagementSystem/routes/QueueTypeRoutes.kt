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
import QueuingManagementSystem.models.*

fun Route.queueTypeRoutes() {
    val authController = QueuingManagementSystem.controllers.AuthController()
    val controller = QueuingManagementSystem.controllers.QueueTypeController()
    route("/queue-types") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val request = call.receive<QueuingManagementSystem.models.QueueTypeRequest>(); val errors = request.validateQueueTypeRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                403,
                false,
                "Department scope violation"
            )
        ); call.respond(HttpStatusCode.OK,
            QueuingManagementSystem.models.IdResponse(
                controller.createQueueType(request),
                QueuingManagementSystem.models.GlobalCredentialResponse(
                    200,
                    true,
                    "Queue type created"
                )
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        put("/update") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val request = call.receive<QueuingManagementSystem.models.QueueTypeRequest>(); if (session.role == QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@put call.respond(HttpStatusCode.Forbidden,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                403,
                false,
                "Department scope violation"
            )
        ); call.respond(HttpStatusCode.OK,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                200,
                controller.updateQueueType(request),
                "Queue type updated"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        get("/company/{companyId}") { try { val companyId = call.parameters["companyId"]?.toIntOrNull() ?: 0; if (companyId <= 0) return@get call.respond(HttpStatusCode.BadRequest,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                400,
                false,
                "companyId is required"
            )
        ); call.respond(HttpStatusCode.OK,
            QueuingManagementSystem.models.ListResponse(
                controller.getQueueTypesByCompanyId(companyId),
                QueuingManagementSystem.models.GlobalCredentialResponse(200, true, "OK")
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        get("/list/{departmentId}") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0; if (session.role == QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name && session.department_id != departmentId) return@get call.respond(HttpStatusCode.Forbidden,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                403,
                false,
                "Department scope violation"
            )
        ); call.respond(HttpStatusCode.OK,
            QueuingManagementSystem.models.ListResponse(
                controller.getQueueTypesByDepartment(departmentId),
                QueuingManagementSystem.models.GlobalCredentialResponse(200, true, "OK")
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
    }
}
