package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.common.Role
import QueuingManagementSystem.common.requireRole
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.AuditController
import QueuingManagementSystem.controllers.DepartmentController
import QueuingManagementSystem.models.validateDepartmentRequest
import QueuingManagementSystem.models.*

fun Route.departmentRoutes() {
    val authController = QueuingManagementSystem.controllers.AuthController()
    val auditController = AuditController()
    val departmentController = QueuingManagementSystem.controllers.DepartmentController()
    route("/departments") {
        post("/create") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireRole(session.role, Role.SUPER_ADMIN)) return@post
                val request = call.receive<QueuingManagementSystem.models.DepartmentRequest>()
                val errors = request.validateDepartmentRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                val createdId = departmentController.createDepartment(request)
                if (createdId > 0) auditController.createAuditLog(session.user_id, createdId, "ADMIN_SCOPE_DEPARTMENT_CREATE", "departments", createdId.toString(), "{\"code\":\"${request.code}\"}")
                call.respond(HttpStatusCode.OK,
                    QueuingManagementSystem.models.IdResponse(
                        createdId,
                        QueuingManagementSystem.models.GlobalCredentialResponse(
                            200,
                            true,
                            "Department created"
                        )
                    )
                )
            } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
                QueuingManagementSystem.models.GlobalCredentialResponse(
                    500,
                    false,
                    e.message ?: "Internal server error"
                )
            ) }
        }
        put("/update") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireRole(session.role, Role.SUPER_ADMIN)) return@put
                val request = call.receive<QueuingManagementSystem.models.DepartmentRequest>()
                val errors = request.validateDepartmentRequest()
                if (errors.isNotEmpty() || request.id == null) return@put call.respond(HttpStatusCode.BadRequest, errors.ifEmpty { mutableListOf(
                    QueuingManagementSystem.models.GlobalCredentialResponse(
                        400,
                        false,
                        "id is required"
                    )
                ) })
                val updated = departmentController.updateDepartment(request)
                if (updated) auditController.createAuditLog(session.user_id, request.id, "ADMIN_SCOPE_DEPARTMENT_UPDATE", "departments", request.id.toString(), "{\"code\":\"${request.code}\"}")
                call.respond(HttpStatusCode.OK,
                    QueuingManagementSystem.models.GlobalCredentialResponse(
                        200,
                        updated,
                        "Department updated"
                    )
                )
            } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
                QueuingManagementSystem.models.GlobalCredentialResponse(
                    500,
                    false,
                    e.message ?: "Internal server error"
                )
            ) }
        }
        get("/list") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.user_id <= 0) return@get call.respond(HttpStatusCode.Unauthorized,
                    QueuingManagementSystem.models.GlobalCredentialResponse(
                        401,
                        false,
                        "Unauthorized"
                    )
                )
                call.respond(HttpStatusCode.OK,
                    QueuingManagementSystem.models.ListResponse(
                        departmentController.getDepartments(),
                        QueuingManagementSystem.models.GlobalCredentialResponse(200, true, "OK")
                    )
                )
            } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
                QueuingManagementSystem.models.GlobalCredentialResponse(
                    500,
                    false,
                    e.message ?: "Internal server error"
                )
            ) }
        }
    }
}
