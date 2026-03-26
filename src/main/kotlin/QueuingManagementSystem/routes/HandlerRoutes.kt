package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.HandlerController
import QueuingManagementSystem.models.validateHandlerRequest
import marlow.systems.queuingsystem.models.*

fun Route.handlerRoutes() {
    val authController = _root_ide_package_.QueuingManagementSystem.controllers.AuthController()
    val controller = _root_ide_package_.QueuingManagementSystem.controllers.HandlerController()
    route("/handlers") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(
                _root_ide_package_.QueuingManagementSystem.common.UserRole.SUPERADMIN.name, _root_ide_package_.QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(403, false, "Forbidden")
        ); val request = call.receive<QueuingManagementSystem.models.HandlerRequest>(); val errors = request.validateHandlerRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == _root_ide_package_.QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                403,
                false,
                "Department scope violation"
            )
        ); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.IdResponse(
                controller.createHandler(request),
                _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(200, true, "Handler created")
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        put("/update") { try { val request = call.receive<QueuingManagementSystem.models.HandlerRequest>(); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                200,
                controller.updateHandler(request),
                "Handler updated"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        post("/start-session") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(
                _root_ide_package_.QueuingManagementSystem.common.UserRole.HANDLER.name, _root_ide_package_.QueuingManagementSystem.common.UserRole.SUPERADMIN.name, _root_ide_package_.QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(403, false, "Forbidden")
        ); val request = call.receive<QueuingManagementSystem.models.HandlerSessionRequest>(); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                200,
                controller.startSession(request.handler_id, request.window_id),
                "Session started"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        post("/end-session") { try { val request = call.receive<QueuingManagementSystem.models.HandlerSessionRequest>(); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                200,
                controller.endSession(request.handler_id),
                "Session ended"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        get("/list/{departmentId}") { try { val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0; call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.ListResponse(
                controller.getHandlersByDepartment(departmentId),
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
