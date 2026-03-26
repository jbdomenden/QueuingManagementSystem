package marlow.systems.queuingsystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import marlow.systems.queuingsystem.common.UserRole
import marlow.systems.queuingsystem.common.extractBearerToken
import marlow.systems.queuingsystem.controllers.AuthController
import marlow.systems.queuingsystem.controllers.DepartmentController
import marlow.systems.queuingsystem.models.*

fun Route.departmentRoutes() {
    val authController = AuthController()
    val departmentController = DepartmentController()
    route("/departments") {
        post("/create") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role != UserRole.SUPERADMIN.name) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                val request = call.receive<DepartmentRequest>()
                val errors = request.validateDepartmentRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                call.respond(HttpStatusCode.OK, IdResponse(departmentController.createDepartment(request), GlobalCredentialResponse(200, true, "Department created")))
            } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) }
        }
        put("/update") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.role != UserRole.SUPERADMIN.name) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                val request = call.receive<DepartmentRequest>()
                val errors = request.validateDepartmentRequest()
                if (errors.isNotEmpty() || request.id == null) return@put call.respond(HttpStatusCode.BadRequest, errors.ifEmpty { mutableListOf(GlobalCredentialResponse(400, false, "id is required")) })
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, departmentController.updateDepartment(request), "Department updated"))
            } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) }
        }
        get("/list") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (session.user_id <= 0) return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                call.respond(HttpStatusCode.OK, ListResponse(departmentController.getDepartments(), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) }
        }
    }
}
