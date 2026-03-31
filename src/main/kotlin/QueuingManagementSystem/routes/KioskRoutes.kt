package QueuingManagementSystem.routes

import QueuingManagementSystem.common.canAccessDepartment
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.common.isSuperAdmin
import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.KioskController
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.IdResponse
import QueuingManagementSystem.models.KioskQueueTypeAssignmentRequest
import QueuingManagementSystem.models.KioskRequest
import QueuingManagementSystem.models.ListResponse
import QueuingManagementSystem.models.validateKioskRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.kioskRoutes() {
    val authController = AuthController()
    val controller = KioskController()

    suspend fun io.ktor.server.routing.RoutingContext.requireKioskAdminSession(): AuthController.ValidatedSession? {
        val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                return null
            }
        if (!session.isSuperAdmin() && session.role != UserRole.DEPARTMENT_ADMIN.name) {
            call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            return null
        }
        return session
    }

    route("/kiosks") {
        post("/create") {
            try {
                val session = requireKioskAdminSession() ?: return@post
                val request = call.receive<KioskRequest>()
                val errors = request.validateKioskRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                if (!session.canAccessDepartment(request.department_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }
                call.respond(
                    HttpStatusCode.OK,
                    IdResponse(controller.createKiosk(request), GlobalCredentialResponse(200, true, "Kiosk created"))
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update") {
            try {
                val session = requireKioskAdminSession() ?: return@put
                val request = call.receive<KioskRequest>()
                val errors = request.validateKioskRequest()
                if (errors.isNotEmpty()) return@put call.respond(HttpStatusCode.BadRequest, errors)

                val existingDepartmentId = request.id?.let { controller.getKioskDepartmentById(it) }
                    ?: return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                if (!session.canAccessDepartment(existingDepartmentId) || !session.canAccessDepartment(request.department_id)) {
                    return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.updateKiosk(request), "Kiosk updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/assign-queue-types") {
            try {
                val session = requireKioskAdminSession() ?: return@post
                val request = call.receive<KioskQueueTypeAssignmentRequest>()
                val kioskDepartmentId = controller.getKioskDepartmentById(request.kiosk_id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Kiosk not found"))
                if (!session.canAccessDepartment(kioskDepartmentId)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }
                val outOfScopeQueueType = request.queue_type_ids.firstOrNull { queueTypeId ->
                    val queueTypeDepartmentId = controller.getQueueTypeDepartmentById(queueTypeId)
                    queueTypeDepartmentId == null || queueTypeDepartmentId != kioskDepartmentId || !session.canAccessDepartment(queueTypeDepartmentId)
                }
                if (outOfScopeQueueType != null) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Queue type scope violation"))
                }
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.assignQueueTypes(request), "Kiosk queue types updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list") {
            try {
                val session = requireKioskAdminSession() ?: return@get
                val data = controller.getKiosks().filter { session.canAccessDepartment(it.department_id) }
                call.respond(HttpStatusCode.OK, ListResponse(data, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
