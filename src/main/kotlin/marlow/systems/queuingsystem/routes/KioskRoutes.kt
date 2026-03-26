package marlow.systems.queuingsystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import marlow.systems.queuingsystem.common.UserRole
import marlow.systems.queuingsystem.common.extractBearerToken
import marlow.systems.queuingsystem.controllers.AuthController
import marlow.systems.queuingsystem.controllers.KioskController
import marlow.systems.queuingsystem.models.*

fun Route.kioskRoutes() {
    val authController = AuthController()
    val controller = KioskController()
    route("/kiosks") {
        post("/create") { try { val session = authController.getUserSessionByToken(call.request.extractBearerToken()); if (session.role !in listOf(UserRole.SUPERADMIN.name, UserRole.DEPARTMENT_ADMIN.name)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden")); val request = call.receive<KioskRequest>(); val errors = request.validateKioskRequest(); if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors); if (session.role == UserRole.DEPARTMENT_ADMIN.name && session.department_id != request.department_id) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation")); call.respond(HttpStatusCode.OK, IdResponse(controller.createKiosk(request), GlobalCredentialResponse(200, true, "Kiosk created"))) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        put("/update") { try { val request = call.receive<KioskRequest>(); call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.updateKiosk(request), "Kiosk updated")) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        post("/assign-queue-types") { try { val request = call.receive<KioskQueueTypeAssignmentRequest>(); call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.assignQueueTypes(request), "Kiosk queue types updated")) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
        get("/list") { try { call.respond(HttpStatusCode.OK, ListResponse(controller.getKiosks(), GlobalCredentialResponse(200, true, "OK"))) } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")) } }
    }
}
