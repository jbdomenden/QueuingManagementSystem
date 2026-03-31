package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.common.UserRole
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

    suspend fun RoutingContext.requirePermissionOrOverride(session: AuthController.ValidatedSession, permission: String): Boolean {
        if (session.permissions.contains(permission) || session.permissions.contains("supervisor_override")) return true
        call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Missing permission: $permission"))
        return false
    }

    suspend fun RoutingContext.resolveHandlerSession(): Pair<AuthController.ValidatedSession, Int>? {
        val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                return null
            }
        val handler = handlerController.getActiveHandlerByUserId(session.userId)
            ?: run {
                call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler context required"))
                return null
            }
        return Pair(session, handler.id)
    }

    route("/tickets") {
        post("/create") {
            try {
                val request = call.receive<TicketCreateRequest>()
                val errors = request.validateTicketCreateRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)

                val response = controller.createTicketWithPrintable(request)
                if (!response.result.Access) return@post call.respond(HttpStatusCode.BadRequest, response)

                val displayIds = controller.getDisplayIdsForQueueType(request.queue_type_id ?: response.ticket.queue_type_id)
                eventPublisher.notifyHandlersTicketCreated(response.ticket.queue_type_id)
                eventPublisher.notifyDisplayTicketCreated(displayIds)
                eventPublisher.notifyAdminDepartmentSummary(response.ticket.department_id)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/handler/context") {
            val resolved = resolveHandlerSession() ?: return@get
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_call_next")) return@get
            val response = controller.getHandlerActiveContext(handlerId)
            call.respond(if (response.result.Access) HttpStatusCode.OK else HttpStatusCode.NotFound, response)
        }

        get("/handler/dashboard") {
            val resolved = resolveHandlerSession() ?: return@get
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_call_next")) return@get
            call.respond(HttpStatusCode.OK, controller.getHandlerDashboardMetrics(handlerId))
        }

        get("/handler/active-ticket") {
            val resolved = resolveHandlerSession() ?: return@get
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_call_next")) return@get
            val active = controller.getCurrentActiveTicket(handlerId)
            call.respond(HttpStatusCode.OK, TicketLifecycleResponse(active, "ACTIVE_TICKET", GlobalCredentialResponse(200, true, "OK")))
        }

        get("/handler/history") {
            val resolved = resolveHandlerSession() ?: return@get
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_call_next")) return@get
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 50).coerceIn(1, 200)
            val offset = (call.request.queryParameters["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
            call.respond(HttpStatusCode.OK, ListResponse(controller.getUserTicketHistory(handlerId, limit, offset), GlobalCredentialResponse(200, true, "OK")))
        }

        post("/handler/call-next") {
            val resolved = resolveHandlerSession() ?: return@post
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_call_next")) return@post
            val result = controller.callNextTicket(session.userId, handlerId)
            if (result.response.result.Access) {
                eventPublisher.notifyDisplayTicketCalled(result.displayIds)
                if (result.departmentId != null) eventPublisher.notifyAdminDepartmentSummary(result.departmentId)
            }
            call.respond(if (result.response.result.Access) HttpStatusCode.OK else HttpStatusCode.Conflict, result.response)
        }

        post("/handler/recall") {
            val resolved = resolveHandlerSession() ?: return@post
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_recall")) return@post
            val request = call.receive<TicketStatusChangeRequest>()
            val errors = request.validate()
            if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
            if (request.handler_id != handlerId) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
            val result = controller.recallTicket(session.userId, handlerId, request.ticket_id, request.reason)
            if (result.response.result.Access) eventPublisher.notifyDisplayTicketRecalled(result.displayIds)
            call.respond(if (result.response.result.Access) HttpStatusCode.OK else HttpStatusCode.Conflict, result.response)
        }

        post("/handler/hold") {
            val resolved = resolveHandlerSession() ?: return@post
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_hold")) return@post
            val request = call.receive<TicketStatusChangeRequest>()
            val errors = request.validate(requireReason = true)
            if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
            if (request.handler_id != handlerId) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
            val result = controller.holdTicket(session.userId, handlerId, request.ticket_id, request.reason)
            if (result.response.result.Access) eventPublisher.notifyDisplayTicketSkipped(result.displayIds)
            call.respond(if (result.response.result.Access) HttpStatusCode.OK else HttpStatusCode.Conflict, result.response)
        }

        post("/handler/no-show") {
            val resolved = resolveHandlerSession() ?: return@post
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_no_show")) return@post
            val request = call.receive<TicketStatusChangeRequest>()
            val errors = request.validate(requireReason = true)
            if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
            if (request.handler_id != handlerId) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
            val result = controller.noShowTicket(session.userId, handlerId, request.ticket_id, request.reason)
            if (result.response.result.Access) eventPublisher.notifyDisplayTicketSkipped(result.displayIds)
            call.respond(if (result.response.result.Access) HttpStatusCode.OK else HttpStatusCode.Conflict, result.response)
        }

        post("/handler/transfer") {
            val resolved = resolveHandlerSession() ?: return@post
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_transfer")) return@post
            val request = call.receive<TicketTransferRequest>()
            val errors = request.validate()
            if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
            if (request.handler_id != handlerId) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
            val result = controller.transferTicket(session.userId, request)
            if (result.response.result.Access) eventPublisher.notifyDisplayTicketSkipped(result.displayIds)
            call.respond(if (result.response.result.Access) HttpStatusCode.OK else HttpStatusCode.Conflict, result.response)
        }

        post("/handler/complete") {
            val resolved = resolveHandlerSession() ?: return@post
            val (session, handlerId) = resolved
            if (!requirePermissionOrOverride(session, "handler_complete")) return@post
            val request = call.receive<TicketStatusChangeRequest>()
            val errors = request.validate()
            if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
            if (request.handler_id != handlerId) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Handler scope violation"))
            val result = controller.completeTicket(session.userId, handlerId, request.ticket_id, request.reason)
            if (result.response.result.Access) {
                eventPublisher.notifyDisplayTicketCompleted(result.displayIds)
                if (result.departmentId != null) eventPublisher.notifyAdminDepartmentSummary(result.departmentId)
            }
            call.respond(if (result.response.result.Access) HttpStatusCode.OK else HttpStatusCode.Conflict, result.response)
        }

        post("/cancel") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!(session.permissions.contains("ticket_cancel") || session.permissions.contains("supervisor_override"))) {
                return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }
            val request = call.receive<TicketCancelRequest>()
            val errors = request.validate()
            if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)

            val handlerId = request.handler_id ?: handlerController.getActiveHandlerByUserId(session.userId)?.id
            val result = controller.cancelTicket(session.userId, handlerId, request.ticket_id, request.reason)
            if (result.response.result.Access) {
                eventPublisher.notifyDisplayTicketSkipped(result.displayIds)
                if (result.departmentId != null) eventPublisher.notifyAdminDepartmentSummary(result.departmentId)
            }
            call.respond(if (result.response.result.Access) HttpStatusCode.OK else HttpStatusCode.Conflict, result.response)
        }


        post("/archive/day") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("archive_manage")) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<ArchiveDayRequest>()
                val departmentId = request.departmentId ?: session.departmentId
                if (request.departmentId != null && !session.permissions.contains("report_view_global") && !session.departmentScopes.contains(request.departmentId)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }
                val archivedCount = controller.archiveQueuesByServiceDate(request.serviceDate, session.userId, departmentId)
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
    }
}
