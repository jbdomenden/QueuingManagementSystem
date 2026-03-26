package QueuingManagementSystem.routes

import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.HandlerController
import QueuingManagementSystem.controllers.TicketController
import QueuingManagementSystem.models.*
import QueuingManagementSystem.realtime.EventPublisher
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.ticketRoutes() {
    val authController = AuthController()
    val handlerController = HandlerController()
    val controller = TicketController()
    val eventPublisher = EventPublisher()

    route("/tickets") {
        post("/create") {
            try {
                val request = call.receive<TicketCreateRequest>()
                val errors = request.validateTicketCreateRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)

                val ticket = controller.createTicket(request)
                if (ticket.id <= 0) return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Ticket create failed"))

                val displayIds = controller.getDisplayIdsForQueueType(request.queue_type_id)
                eventPublisher.notifyHandlersTicketCreated(request.queue_type_id)
                eventPublisher.notifyDisplayTicketCreated(displayIds)
                eventPublisher.notifyAdminDepartmentSummary(ticket.department_id)
                call.respond(HttpStatusCode.OK, ticket)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/live/{departmentId}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0
                if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != departmentId) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                call.respond(HttpStatusCode.OK, ListResponse(controller.getLiveTickets(departmentId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/call-next") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.HANDLER.name, UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<CallNextRequest>()
                val handler = handlerController.getActiveHandlerByUserId(session.user_id)
                if (session.role == UserRole.HANDLER.name && (handler == null || handler.id != request.handler_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
                }

                val ticket = controller.callNext(request.handler_id)
                if (ticket.id > 0) {
                    val displayIds = controller.getDisplayIdsByHandler(request.handler_id)
                    eventPublisher.notifyDisplayTicketCalled(displayIds)
                    eventPublisher.notifyAdminDepartmentSummary(ticket.department_id)
                }
                call.respond(HttpStatusCode.OK, ticket)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/start-service") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.HANDLER.name, UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<TicketActionRequest>()
                val handler = handlerController.getActiveHandlerByUserId(session.user_id)
                if (session.role == UserRole.HANDLER.name && (handler == null || handler.id != request.handler_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
                }

                val updated = controller.updateTicketStatus(request.ticket_id, request.handler_id, "IN_SERVICE", request.notes)
                if (updated) eventPublisher.notifyDisplayTicketCalled(controller.getDisplayIdsByHandler(request.handler_id))
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, updated, "Ticket set to IN_SERVICE"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/skip") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.HANDLER.name, UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<TicketActionRequest>()
                val handler = handlerController.getActiveHandlerByUserId(session.user_id)
                if (session.role == UserRole.HANDLER.name && (handler == null || handler.id != request.handler_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
                }

                val updated = controller.updateTicketStatus(request.ticket_id, request.handler_id, "SKIPPED", request.notes)
                if (updated) eventPublisher.notifyDisplayTicketSkipped(controller.getDisplayIdsByHandler(request.handler_id))
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, updated, "Ticket skipped"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/recall") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.HANDLER.name, UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<TicketActionRequest>()
                val handler = handlerController.getActiveHandlerByUserId(session.user_id)
                if (session.role == UserRole.HANDLER.name && (handler == null || handler.id != request.handler_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
                }

                val updated = controller.updateTicketStatus(request.ticket_id, request.handler_id, "CALLED", request.notes)
                if (updated) eventPublisher.notifyDisplayTicketRecalled(controller.getDisplayIdsByHandler(request.handler_id))
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, updated, "Ticket recalled"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/complete") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.HANDLER.name, UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<TicketActionRequest>()
                val handler = handlerController.getActiveHandlerByUserId(session.user_id)
                if (session.role == UserRole.HANDLER.name && (handler == null || handler.id != request.handler_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
                }

                val updated = controller.updateTicketStatus(request.ticket_id, request.handler_id, "COMPLETED", request.notes)
                if (updated) {
                    val displayIds = controller.getDisplayIdsByHandler(request.handler_id)
                    eventPublisher.notifyDisplayTicketCompleted(displayIds)
                    if (session.department_id != null) eventPublisher.notifyAdminDepartmentSummary(session.department_id)
                }
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, updated, "Ticket completed"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
