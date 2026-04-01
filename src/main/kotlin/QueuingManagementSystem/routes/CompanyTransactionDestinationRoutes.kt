package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.devices.requireDeviceContext
import QueuingManagementSystem.devices.DeviceType
import QueuingManagementSystem.common.Role
import QueuingManagementSystem.common.requireAnyRole
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.CompanyTransactionDestinationController
import QueuingManagementSystem.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.companyTransactionDestinationRoutes() {
    val authController = AuthController()
    val controller = CompanyTransactionDestinationController()

    route("/company-transaction-destinations") {
        get("/kiosk/company-transaction/{companyTransactionId}") {
            try {
                requireDeviceContext(DeviceType.KIOSK) ?: return@get
                val companyTransactionId = call.parameters["companyTransactionId"]?.toIntOrNull() ?: 0
                if (companyTransactionId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "companyTransactionId is required"))
                call.respond(HttpStatusCode.OK, CompanyTransactionDestinationKioskListResponse(controller.getActiveCompanyTransactionDestinationsForKiosk(companyTransactionId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/company-transaction/{companyTransactionId}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@get
                val companyTransactionId = call.parameters["companyTransactionId"]?.toIntOrNull() ?: 0
                if (companyTransactionId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "companyTransactionId is required"))
                call.respond(HttpStatusCode.OK, CompanyTransactionDestinationListResponse(controller.getCompanyTransactionDestinationsByTransactionId(companyTransactionId), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/{id}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@get
                val id = call.parameters["id"]?.toIntOrNull() ?: 0
                if (id <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                val item = controller.getCompanyTransactionDestinationById(id)
                if (item.id <= 0) return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Destination not found"))
                call.respond(HttpStatusCode.OK, CompanyTransactionDestinationResponse(item, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/create") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@post
                val request = call.receive<CompanyTransactionDestinationRequest>()
                val errors = request.validateCompanyTransactionDestinationRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                val id = controller.postCompanyTransactionDestination(request)
                call.respond(HttpStatusCode.OK, IdResponse(id, GlobalCredentialResponse(200, id > 0, "Destination created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update/{id}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@put
                val id = call.parameters["id"]?.toIntOrNull() ?: 0
                if (id <= 0) return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                val request = call.receive<CompanyTransactionDestinationRequest>()
                val errors = request.validateCompanyTransactionDestinationRequest()
                if (errors.isNotEmpty()) return@put call.respond(HttpStatusCode.BadRequest, errors)
                val affected = controller.updateCompanyTransactionDestination(id, request)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Destination updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        patch("/toggle/{id}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@patch
                val id = call.parameters["id"]?.toIntOrNull() ?: 0
                if (id <= 0) return@patch call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                val request = call.receive<CompanyTransactionDestinationToggleRequest>()
                val errors = request.validateCompanyTransactionDestinationToggleRequest()
                if (errors.isNotEmpty()) return@patch call.respond(HttpStatusCode.BadRequest, errors)
                val affected = controller.toggleCompanyTransactionDestinationStatus(id, request.status)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Destination status updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        delete("/deactivate/{id}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@delete
                val id = call.parameters["id"]?.toIntOrNull() ?: 0
                if (id <= 0) return@delete call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                val affected = controller.deactivateCompanyTransactionDestination(id)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Destination deactivated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
