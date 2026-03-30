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

                val response = controller.createTicketWithPrintable(request)
                if (!response.result.Access) return@post call.respond(HttpStatusCode.BadRequest, response)

                val displayIds = controller.getDisplayIdsForQueueType(request.queue_type_id)
                eventPublisher.notifyHandlersTicketCreated(request.queue_type_id)
                eventPublisher.notifyDisplayTicketCreated(displayIds)
                eventPublisher.notifyAdminDepartmentSummary(response.ticket.department_id)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/{ticketId}/printable") {
            try {
                val ticketId = call.parameters["ticketId"]?.toIntOrNull() ?: 0
                if (ticketId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "ticketId is required"))
                val printable = controller.getPrintableTicketDetails(ticketId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Ticket not found"))
                call.respond(HttpStatusCode.OK, printable)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/archive/day") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<ArchiveDayRequest>()
                val departmentId = if (session.role == UserRole.DEPARTMENT_ADMIN.name) session.department_id else request.departmentId
                val archivedCount = controller.archiveQueuesByServiceDate(request.serviceDate, session.user_id, departmentId)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, true, "Archived tickets count: $archivedCount"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/archived") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }
                val dateFrom = call.request.queryParameters["dateFrom"] ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateFrom is required"))
                val dateTo = call.request.queryParameters["dateTo"] ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateTo is required"))
                val queueTypeId = call.request.queryParameters["queueTypeId"]?.toIntOrNull()
                val status = call.request.queryParameters["status"]

                val data = if (session.role == UserRole.DEPARTMENT_ADMIN.name) {
                    controller.getArchivedTicketsByDepartment(session.department_id ?: 0, dateFrom, dateTo, queueTypeId, status)
                } else {
                    val departmentId = call.request.queryParameters["departmentId"]?.toIntOrNull()
                    if (departmentId != null) controller.getArchivedTicketsByDepartment(departmentId, dateFrom, dateTo, queueTypeId, status)
                    else controller.getArchivedTickets(dateFrom, dateTo, queueTypeId, status)
                }
                call.respond(HttpStatusCode.OK, ListResponse(data, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/archived/{ticketId}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }
                val ticketId = call.parameters["ticketId"]?.toIntOrNull() ?: 0
                if (ticketId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "ticketId is required"))

                val item = controller.getArchivedTicketById(ticketId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Archived ticket not found"))
                if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != item.department_id) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }
                call.respond(HttpStatusCode.OK, item)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/live/{departmentId}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }
                val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0
                if (departmentId <= 0) {
                    return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
                }
                if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != departmentId) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }
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
