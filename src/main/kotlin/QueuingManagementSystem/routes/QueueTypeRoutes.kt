package QueuingManagementSystem.routes

import QueuingManagementSystem.common.canAccessDepartment
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.AuditController
import QueuingManagementSystem.controllers.QueueTypeController
import QueuingManagementSystem.models.*
import QueuingManagementSystem.models.validateQueueTypeRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.queueTypeRoutes() {
    val authController = AuthController()
    val auditController = AuditController()
    val controller = QueueTypeController()

    route("/queue-types") {
        post("/create") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("queue_type_manage")) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                val request = call.receive<QueueTypeRequest>()
                val errors = request.validateQueueTypeRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                if (!session.canAccessDepartment(request.department_id)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))

                val createdId = controller.createQueueType(request)
                if (createdId > 0) {
                    auditController.createAuditLog(session.userId, request.department_id, "ADMIN_SCOPE_QUEUE_TYPE_CREATE", "queue_types", createdId.toString(), "{\"department_id\":${request.department_id}}")
                }
                call.respond(HttpStatusCode.OK, IdResponse(createdId, GlobalCredentialResponse(200, true, "Queue type created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("queue_type_manage")) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                val request = call.receive<QueueTypeRequest>()
                val existing = request.id?.let { controller.getQueueTypeById(it) }
                    ?: return@put call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Queue type not found"))
                if (!session.canAccessDepartment(existing.department_id) || !session.canAccessDepartment(request.department_id)) {
                    return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                val updated = controller.updateQueueType(request)
                if (updated && request.id != null) {
                    auditController.createAuditLog(session.userId, request.department_id, "ADMIN_SCOPE_QUEUE_TYPE_UPDATE", "queue_types", request.id.toString(), "{\"department_id\":${request.department_id}}")
                }
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, updated, "Queue type updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/company/{companyId}") {
            try {
                val companyId = call.parameters["companyId"]?.toIntOrNull() ?: 0
                if (companyId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "companyId is required"))
                call.respond(HttpStatusCode.OK, ListResponse(controller.getQueueTypesByCompanyId(companyId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list/{departmentId}") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("queue_type_manage")) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0
                if (departmentId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
                if (!session.canAccessDepartment(departmentId)) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))

                call.respond(HttpStatusCode.OK, ListResponse(controller.getQueueTypesByDepartment(departmentId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
