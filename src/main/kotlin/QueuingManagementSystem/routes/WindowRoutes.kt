package QueuingManagementSystem.routes

import QueuingManagementSystem.common.canAccessDepartment
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.WindowController
import QueuingManagementSystem.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.windowRoutes() {
    val authController = AuthController()
    val controller = WindowController()
    route("/windows") {
        post("/create") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("window_manage")) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                val request = call.receive<WindowRequest>()
                val errors = request.validateWindowRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                if (!session.canAccessDepartment(request.department_id)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                call.respond(HttpStatusCode.OK, IdResponse(controller.createWindow(request), GlobalCredentialResponse(200, true, "Window created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("window_manage")) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                val request = call.receive<WindowRequest>()

                val departmentId = request.id?.let { controller.getWindowDepartmentId(it) } ?: request.department_id
                if (departmentId == null || !session.canAccessDepartment(departmentId)) {
                    return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.updateWindow(request), "Window updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list/{departmentId}") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("window_manage")) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))

                val departmentId = call.parameters["departmentId"]?.toIntOrNull() ?: 0
                if (departmentId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required"))
                if (!session.canAccessDepartment(departmentId)) return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                call.respond(HttpStatusCode.OK, ListResponse(controller.getWindowsByDepartment(departmentId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/assign-queue-types") {
            try {
                val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                if (!session.permissions.contains("window_manage")) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                val request = call.receive<WindowQueueTypeAssignmentRequest>()

                val windowDepartmentId = controller.getWindowDepartmentId(request.window_id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Window not found"))
                if (!session.canAccessDepartment(windowDepartmentId)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))

                val outOfScopeQueueType = request.queue_type_ids.firstOrNull { queueTypeId ->
                    val queueTypeDepartmentId = controller.getQueueTypeDepartmentId(queueTypeId)
                    queueTypeDepartmentId == null || queueTypeDepartmentId != windowDepartmentId || !session.canAccessDepartment(queueTypeDepartmentId)
                }
                if (outOfScopeQueueType != null) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Queue type scope violation"))

                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.assignQueueTypes(request), "Window queue types updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
