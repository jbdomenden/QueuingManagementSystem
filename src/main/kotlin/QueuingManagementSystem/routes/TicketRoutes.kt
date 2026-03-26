package QueuingManagementSystem.routes

import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.TicketController
import QueuingManagementSystem.models.CallNextRequest
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.ListResponse
import QueuingManagementSystem.models.TicketActionRequest
import QueuingManagementSystem.models.TicketCreateRequest
import QueuingManagementSystem.models.validateTicketCreateRequest
import QueuingManagementSystem.realtime.EventPublisher
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.ticketRoutes() {
    val authController = AuthController()
    val controller = TicketController()
    val eventPublisher = EventPublisher()
    route("/tickets") {
        post("/create") {
            try {
                val request = call.receive<TicketCreateRequest>()
                val errors = request.validateTicketCreateRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                val ticket = controller.createTicket(request)
                if (ticket.id <= 0) return@post call.respond(HttpStatusCode.BadRequest,
                    GlobalCredentialResponse(400, false, "Ticket create failed")
                )
                eventPublisher.publishNewTicket(request.queue_type_id, emptyList())
                call.respond(HttpStatusCode.OK, ticket)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
                )
            }
        }
        get("/live/{departmentId}") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0; if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != departmentId) return@get call.respond(HttpStatusCode.Forbidden,
            GlobalCredentialResponse(403, false, "Department scope violation")
        ); call.respond(HttpStatusCode.OK,
            ListResponse(controller.getLiveTickets(departmentId), GlobalCredentialResponse(200, true, "OK"))
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        post("/call-next") { try { val request = call.receive<CallNextRequest>(); call.respond(HttpStatusCode.OK, controller.callNext(request.handler_id)) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        post("/start-service") { try { val request = call.receive<TicketActionRequest>(); call.respond(HttpStatusCode.OK,
            GlobalCredentialResponse(
                200,
                controller.updateTicketStatus(request.ticket_id, request.handler_id, "IN_SERVICE"),
                "Ticket set to IN_SERVICE"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        post("/skip") { try { val request = call.receive<TicketActionRequest>(); call.respond(HttpStatusCode.OK,
            GlobalCredentialResponse(
                200,
                controller.updateTicketStatus(request.ticket_id, request.handler_id, "SKIPPED"),
                "Ticket skipped"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        post("/recall") { try { val request = call.receive<TicketActionRequest>(); call.respond(HttpStatusCode.OK,
            GlobalCredentialResponse(
                200,
                controller.updateTicketStatus(request.ticket_id, request.handler_id, "CALLED"),
                "Ticket recalled"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        post("/complete") { try { val request = call.receive<TicketActionRequest>(); call.respond(HttpStatusCode.OK,
            GlobalCredentialResponse(
                200,
                controller.updateTicketStatus(request.ticket_id, request.handler_id, "COMPLETED"),
                "Ticket completed"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
    }
}
