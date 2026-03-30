package QueuingManagementSystem.routes

import QueuingManagementSystem.common.UserRole
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
        get("/department-summary/{departmentId}") {
            val session = authController.getUserSessionByToken(call.request.extractBearerToken())
            if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }

            val id = call.parameters["departmentId"]?.toIntOrNull() ?: 0
            if (id <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
            if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != id) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
            }
            call.respond(HttpStatusCode.OK, controller.getDepartmentSummary(id))
        }

        get("/handler-performance/{departmentId}") {
            val session = authController.getUserSessionByToken(call.request.extractBearerToken())
            if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }

            val id = call.parameters["departmentId"]?.toIntOrNull() ?: 0
            if (id <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
            if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != id) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
            }
            call.respond(HttpStatusCode.OK, controller.getHandlerPerformance(id))
        }

        get("/queue-volume/{departmentId}") {
            val session = authController.getUserSessionByToken(call.request.extractBearerToken())
            if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }

            val id = call.parameters["departmentId"]?.toIntOrNull() ?: 0
            if (id <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
            if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != id) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
            }
            call.respond(HttpStatusCode.OK, controller.getQueueVolume(id))
        }

        get("/archived-queues") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val dateFrom = call.request.queryParameters["dateFrom"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateFrom is required"))
                val dateTo = call.request.queryParameters["dateTo"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateTo is required"))

                val data = if (session.role == UserRole.DEPARTMENT_ADMIN.name) {
                    controller.getArchivedQueueReportByDepartment(session.department_id ?: 0, dateFrom, dateTo)
                } else {
                    val departmentId = call.request.queryParameters["departmentId"]?.toIntOrNull()
                    if (departmentId != null) controller.getArchivedQueueReportByDepartment(departmentId, dateFrom, dateTo)
                    else controller.getArchivedQueueReport(dateFrom, dateTo)
                }
                call.respond(HttpStatusCode.OK, ListResponse(data, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/department-archived-summary") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val dateFrom = call.request.queryParameters["dateFrom"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateFrom is required"))
                val dateTo = call.request.queryParameters["dateTo"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "dateTo is required"))

                val departmentId = if (session.role == UserRole.DEPARTMENT_ADMIN.name) {
                    session.department_id ?: 0
                } else {
                    call.request.queryParameters["departmentId"]?.toIntOrNull() ?: 0
                }

                if (departmentId <= 0) {
                    return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
                }

                val report = controller.getArchivedQueueReportByDepartment(departmentId, dateFrom, dateTo)
                call.respond(HttpStatusCode.OK, ListResponse(report, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
