package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import QueuingManagementSystem.controllers.DisplayController
import QueuingManagementSystem.models.validateDisplayBoardRequest
import marlow.systems.queuingsystem.models.*

fun Route.displayRoutes() {
    val controller = _root_ide_package_.QueuingManagementSystem.controllers.DisplayController()
    route("/displays") {
        post("/create") { try { val request = call.receive<QueuingManagementSystem.models.DisplayBoardRequest>(); val errors = request.validateDisplayBoardRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.IdResponse(
                controller.createDisplayBoard(request),
                _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(200, true, "Display created")
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        put("/update") { try { val request = call.receive<QueuingManagementSystem.models.DisplayBoardRequest>(); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                200,
                controller.updateDisplayBoard(request),
                "Display updated"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        post("/assign-windows") { try { val request = call.receive<QueuingManagementSystem.models.DisplayBoardWindowAssignmentRequest>(); call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                200,
                controller.assignWindows(request),
                "Display windows assigned"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        get("/snapshot/{displayId}") { try { val displayId = call.parameters["displayId"]?.toIntOrNull() ?: 0; call.respond(HttpStatusCode.OK, controller.getDisplaySnapshot(displayId)) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        get("/list") { try { call.respond(HttpStatusCode.OK,
            _root_ide_package_.QueuingManagementSystem.models.ListResponse(
                controller.getDisplayBoards(),
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
