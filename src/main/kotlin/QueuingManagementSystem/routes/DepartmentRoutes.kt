package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.DepartmentController
import QueuingManagementSystem.models.validateDepartmentRequest
import QueuingManagementSystem.models.*

fun Route.departmentRoutes() {
    val authController = QueuingManagementSystem.controllers.AuthController()
    val departmentController = QueuingManagementSystem.controllers.DepartmentController()
    route("/departments") {
        post("/create") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role != QueuingManagementSystem.common.UserRole.SUPERADMIN.name) return@post call.respond(HttpStatusCode.Forbidden,
                    QueuingManagementSystem.models.GlobalCredentialResponse(403, false, "Forbidden")
                )
                val request = call.receive<QueuingManagementSystem.models.DepartmentRequest>()
                val errors = request.validateDepartmentRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                call.respond(HttpStatusCode.OK,
                    QueuingManagementSystem.models.IdResponse(
                        departmentController.createDepartment(request),
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
                if (session.role != QueuingManagementSystem.common.UserRole.SUPERADMIN.name) return@put call.respond(HttpStatusCode.Forbidden,
                    QueuingManagementSystem.models.GlobalCredentialResponse(403, false, "Forbidden")
                )
                val request = call.receive<QueuingManagementSystem.models.DepartmentRequest>()
                val errors = request.validateDepartmentRequest()
                if (errors.isNotEmpty() || request.id == null) return@put call.respond(HttpStatusCode.BadRequest, errors.ifEmpty { mutableListOf(
                    QueuingManagementSystem.models.GlobalCredentialResponse(
                        400,
                        false,
                        "id is required"
                    )
                ) })
                call.respond(HttpStatusCode.OK,
                    QueuingManagementSystem.models.GlobalCredentialResponse(
                        200,
                        departmentController.updateDepartment(request),
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
