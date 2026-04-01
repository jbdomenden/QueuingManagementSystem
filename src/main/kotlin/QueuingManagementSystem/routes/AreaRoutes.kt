package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.common.canAccessDepartment
import QueuingManagementSystem.common.Role
import QueuingManagementSystem.common.requireAnyRole
import QueuingManagementSystem.controllers.AreaController
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.models.AreaRequest
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.IdResponse
import QueuingManagementSystem.models.ListResponse
import QueuingManagementSystem.models.validateAreaRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.areaRoutes() {
    val authController = AuthController()
    val areaController = AreaController()

    route("/areas") {
        post("/create") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@post

                val request = call.receive<AreaRequest>()
                val errors = request.validateAreaRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                if (!session.canAccessDepartment(request.department_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                call.respond(HttpStatusCode.OK, IdResponse(areaController.createArea(request), GlobalCredentialResponse(200, true, "Area created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@put

                val request = call.receive<AreaRequest>()
                val errors = request.validateAreaRequest()
                if (request.id == null || errors.isNotEmpty()) {
                    val all = errors.toMutableList()
                    if (request.id == null) all.add(GlobalCredentialResponse(400, false, "id is required"))
                    return@put call.respond(HttpStatusCode.BadRequest, all)
                }
                if (!session.canAccessDepartment(request.department_id)) {
                    return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, areaController.updateArea(request), "Area updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list/{departmentId}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0
                if (departmentId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
                if (!session.canAccessDepartment(departmentId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                call.respond(HttpStatusCode.OK, ListResponse(areaController.getAreasByDepartment(departmentId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
