package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.ReportController
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.ListResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.reportRoutes() {
    val authController = AuthController()
    val controller = ReportController()

    route("/reports") {
        fun canViewDepartment(session: AuthController.ValidatedSession, departmentId: Int): Boolean {
            if (session.permissions.contains("report_view_global")) return true
            return session.permissions.contains("report_view_department") && session.departmentScopes.contains(departmentId)
        }

        get("/department-summary/{departmentId}") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            val id = call.parameters["departmentId"]?.toIntOrNull() ?: 0
            if (id <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
            if (!canViewDepartment(session, id)) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }
            call.respond(HttpStatusCode.OK, controller.getDepartmentSummary(id))
        }

        get("/handler-performance/{departmentId}") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            val id = call.parameters["departmentId"]?.toIntOrNull() ?: 0
            if (id <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
            if (!canViewDepartment(session, id)) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            call.respond(HttpStatusCode.OK, controller.getHandlerPerformance(id))
        }

        get("/queue-volume/{departmentId}") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            val id = call.parameters["departmentId"]?.toIntOrNull() ?: 0
            if (id <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
            if (!canViewDepartment(session, id)) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            call.respond(HttpStatusCode.OK, controller.getQueueVolume(id))
        }

        get("/archived-queues") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("report_view_department") && !session.permissions.contains("report_view_global")) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                val dateFrom = call.request.queryParameters["dateFrom"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateFrom is required"))
                val dateTo = call.request.queryParameters["dateTo"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateTo is required"))

                val departmentId = call.request.queryParameters["departmentId"]?.toIntOrNull()
                val data = when {
                    departmentId != null && canViewDepartment(session, departmentId) -> controller.getArchivedQueueReportByDepartment(departmentId, dateFrom, dateTo)
                    departmentId != null -> return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                    session.permissions.contains("report_view_global") -> controller.getArchivedQueueReport(dateFrom, dateTo)
                    else -> controller.getArchivedQueueReportByDepartment(session.departmentScopes.firstOrNull() ?: 0, dateFrom, dateTo)
                }
                call.respond(HttpStatusCode.OK, ListResponse(data, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/department-archived-summary") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("report_view_department") && !session.permissions.contains("report_view_global")) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val dateFrom = call.request.queryParameters["dateFrom"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateFrom is required"))
                val dateTo = call.request.queryParameters["dateTo"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateTo is required"))

                val departmentId = call.request.queryParameters["departmentId"]?.toIntOrNull() ?: session.departmentScopes.firstOrNull() ?: 0

                if (departmentId <= 0) {
                    return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
                }
                if (!canViewDepartment(session, departmentId)) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))

                val report = controller.getArchivedQueueReportByDepartment(departmentId, dateFrom, dateTo)
                call.respond(HttpStatusCode.OK, ListResponse(report, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/daily-archive-metrics") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("report_view_department") && !session.permissions.contains("report_view_global")) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }
                val dateFrom = call.request.queryParameters["dateFrom"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateFrom is required"))
                val dateTo = call.request.queryParameters["dateTo"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateTo is required"))
                val requestedDepartment = call.request.queryParameters["departmentId"]?.toIntOrNull()
                val scopedDepartment = if (requestedDepartment != null) {
                    if (!canViewDepartment(session, requestedDepartment)) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                    requestedDepartment
                } else if (session.permissions.contains("report_view_global")) {
                    null
                } else {
                    session.departmentScopes.firstOrNull()
                }
                val metrics = controller.getDailyArchiveMetrics(dateFrom, dateTo, scopedDepartment)
                call.respond(HttpStatusCode.OK, ListResponse(metrics, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
