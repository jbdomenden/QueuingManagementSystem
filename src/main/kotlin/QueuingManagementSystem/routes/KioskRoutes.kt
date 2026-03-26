package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import QueuingManagementSystem.common.UserRole
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.KioskController
import QueuingManagementSystem.models.validateKioskRequest
import QueuingManagementSystem.models.*

fun Route.kioskRoutes() {
    val authController = QueuingManagementSystem.controllers.AuthController()
    val controller = QueuingManagementSystem.controllers.KioskController()
    route("/kiosks") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(
                QueuingManagementSystem.common.UserRole.SUPERADMIN.name, QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden,
            QueuingManagementSystem.models.GlobalCredentialResponse(403, false, "Forbidden")
        ); val request = call.receive<QueuingManagementSystem.models.KioskRequest>(); val errors = request.validateKioskRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == QueuingManagementSystem.common.UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                403,
                false,
                "Department scope violation"
            )
        ); call.respond(HttpStatusCode.OK,
            QueuingManagementSystem.models.IdResponse(
                controller.createKiosk(request),
                QueuingManagementSystem.models.GlobalCredentialResponse(200, true, "Kiosk created")
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        put("/update") { try { val request = call.receive<QueuingManagementSystem.models.KioskRequest>(); call.respond(HttpStatusCode.OK,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                200,
                controller.updateKiosk(request),
                "Kiosk updated"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        post("/assign-queue-types") { try { val request = call.receive<QueuingManagementSystem.models.KioskQueueTypeAssignmentRequest>(); call.respond(HttpStatusCode.OK,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                200,
                controller.assignQueueTypes(request),
                "Kiosk queue types updated"
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
        get("/list") { try { call.respond(HttpStatusCode.OK,
            QueuingManagementSystem.models.ListResponse(
                controller.getKiosks(),
                QueuingManagementSystem.models.GlobalCredentialResponse(200, true, "OK")
            )
        ) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError,
            QueuingManagementSystem.models.GlobalCredentialResponse(
                500,
                false,
                e.message ?: "Internal server error"
            )
        ) } }
    }
}
