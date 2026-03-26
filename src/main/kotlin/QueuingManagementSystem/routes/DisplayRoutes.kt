package QueuingManagementSystem.routes

import QueuingManagementSystem.controllers.DisplayController
import QueuingManagementSystem.models.DisplayBoardRequest
import QueuingManagementSystem.models.DisplayBoardWindowAssignmentRequest
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.IdResponse
import QueuingManagementSystem.models.ListResponse
import QueuingManagementSystem.models.validateDisplayBoardRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.displayRoutes() {
    val controller = DisplayController()
    route("/displays") {
        post("/create") { try { val request = call.receive<DisplayBoardRequest>(); val errors = request.validateDisplayBoardRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); call.respond(HttpStatusCode.OK,
            IdResponse(controller.createDisplayBoard(request), GlobalCredentialResponse(200, true, "Display created"))
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        put("/update") { try { val request = call.receive<DisplayBoardRequest>(); call.respond(HttpStatusCode.OK,
            GlobalCredentialResponse(200, controller.updateDisplayBoard(request), "Display updated")
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        post("/assign-windows") { try { val request = call.receive<DisplayBoardWindowAssignmentRequest>(); call.respond(HttpStatusCode.OK,
            GlobalCredentialResponse(200, controller.assignWindows(request), "Display windows assigned")
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        get("/snapshot/{displayId}") { try { val displayId = call.parameters["displayId"]?.toIntOrNull() ?: 0; call.respond(HttpStatusCode.OK, controller.getDisplaySnapshot(displayId)) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        get("/list") { try { call.respond(HttpStatusCode.OK,
            ListResponse(controller.getDisplayBoards(), GlobalCredentialResponse(200, true, "OK"))
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
    }
}
