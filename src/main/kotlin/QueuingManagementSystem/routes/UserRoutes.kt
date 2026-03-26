package QueuingManagementSystem.routes

import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.UserController
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.IdResponse
import QueuingManagementSystem.models.ListResponse
import QueuingManagementSystem.models.UserRequest
import QueuingManagementSystem.models.validateUserRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.userRoutes() {
    val authController = AuthController()
    val userController = UserController()
    route("/users") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(
                UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden,
            GlobalCredentialResponse(403, false, "Forbidden")
        ); val request = call.receive<UserRequest>(); val errors = request.validateUserRequest(true); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden,
            GlobalCredentialResponse(403, false, "Department scope violation")
        ); call.respond(HttpStatusCode.OK,
            IdResponse(userController.createUser(request), GlobalCredentialResponse(200, true, "User created"))
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        put("/update") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(
                UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) return@put call.respond(HttpStatusCode.Forbidden,
            GlobalCredentialResponse(403, false, "Forbidden")
        ); val request = call.receive<UserRequest>(); val errors = request.validateUserRequest(false); if (errors.isNotEmpty()) return@put call.respond(HttpStatusCode.BadRequest, errors); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@put call.respond(HttpStatusCode.Forbidden,
            GlobalCredentialResponse(403, false, "Department scope violation")
        ); call.respond(HttpStatusCode.OK,
            GlobalCredentialResponse(200, userController.updateUser(request), "User updated")
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
        get("/list") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.user_id <= 0) return@get call.respond(HttpStatusCode.Unauthorized,
            GlobalCredentialResponse(401, false, "Unauthorized")
        ); val departmentId = if (session.role == UserRole.SUPERADMIN.name) null else session.department_id; call.respond(HttpStatusCode.OK,
            ListResponse(userController.getUsers(departmentId), GlobalCredentialResponse(200, true, "OK"))
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            GlobalCredentialResponse(500, false, e.message ?: "Internal server error")
        ) } }
    }
}
