package QueuingManagementSystem.routes

import QueuingManagementSystem.common.allowedDepartmentIds
import QueuingManagementSystem.common.canAccessDepartment
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.common.isSuperAdmin
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.AuditController
import QueuingManagementSystem.controllers.UserController
import QueuingManagementSystem.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.userRoutes() {
    val authController = AuthController()
    val auditController = AuditController()
    val userController = UserController()

    route("/users") {
        post("/create") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

                val request = call.receive<UserRequest>()
                val errors = request.validateUserRequest(true)
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)

                val hasGlobal = session.permissions.contains("user_manage_global")
                val hasDepartment = session.permissions.contains("user_manage_department")
                if (!hasGlobal && !hasDepartment) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                if (!hasGlobal) {
                    val targetDepartmentId = request.department_id
                        ?: return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "department_id is required for scoped user management"))
                    if (!session.canAccessDepartment(targetDepartmentId)) {
                        return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                    }
                }

                val createdId = userController.createUser(request)
                if (createdId > 0) {
                    auditController.createAuditLog(session.userId, request.department_id, "ADMIN_SCOPE_USER_CREATE", "users", createdId.toString(), "{\"department_id\":${request.department_id}}")
                }
                call.respond(HttpStatusCode.OK, IdResponse(createdId, GlobalCredentialResponse(200, true, "User created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

                val request = call.receive<UserRequest>()
                val errors = request.validateUserRequest(false)
                if (errors.isNotEmpty()) return@put call.respond(HttpStatusCode.BadRequest, errors)

                val hasGlobal = session.permissions.contains("user_manage_global")
                val hasDepartment = session.permissions.contains("user_manage_department")
                if (!hasGlobal && !hasDepartment) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                if (!hasGlobal) {
                    val targetDepartmentId = request.department_id
                        ?: return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "department_id is required for scoped user management"))
                    if (!session.canAccessDepartment(targetDepartmentId)) {
                        return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                    }
                }

                val updated = userController.updateUser(request)
                if (updated && request.id != null) {
                    auditController.createAuditLog(session.userId, request.department_id, "ADMIN_SCOPE_USER_UPDATE", "users", request.id.toString(), "{\"department_id\":${request.department_id}}")
                }
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, updated, "User updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

                val hasGlobal = session.permissions.contains("user_manage_global")
                val hasDepartment = session.permissions.contains("user_manage_department")
                if (!hasGlobal && !hasDepartment) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                val data = userController.getUsers(
                    departmentId = null,
                    allowedDepartmentIds = session.allowedDepartmentIds(),
                    globalAccess = hasGlobal || session.isSuperAdmin()
                )
                call.respond(HttpStatusCode.OK, ListResponse(data, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/department/{departmentId}") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0
                if (departmentId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))

                val hasGlobal = session.permissions.contains("user_manage_global")
                val hasDepartment = session.permissions.contains("user_manage_department")
                if (!hasGlobal && !hasDepartment) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                if (!hasGlobal && !session.canAccessDepartment(departmentId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                call.respond(HttpStatusCode.OK, ListResponse(userController.getUsers(departmentId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/{id}") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

                val userId = call.parameters["id"]?.toIntOrNull() ?: 0
                if (userId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))

                val user = userController.getUserById(userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "User not found"))

                val hasGlobal = session.permissions.contains("user_manage_global")
                val hasDepartment = session.permissions.contains("user_manage_department")
                if (!hasGlobal && !hasDepartment) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                if (!hasGlobal && (user.department_id == null || !session.canAccessDepartment(user.department_id))) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
