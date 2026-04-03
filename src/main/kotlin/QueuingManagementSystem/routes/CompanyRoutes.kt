package QueuingManagementSystem.routes

import QueuingManagementSystem.common.Role
import QueuingManagementSystem.common.normalizeRole
import QueuingManagementSystem.config.ProviderRegistry
import QueuingManagementSystem.controllers.CompanyController
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.devices.DeviceType
import QueuingManagementSystem.models.CompanyKioskBoard
import QueuingManagementSystem.models.CompanyListResponse
import QueuingManagementSystem.models.CompanyRequest
import QueuingManagementSystem.models.CompanyResponse
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.IdResponse
import QueuingManagementSystem.models.SingleResponse
import QueuingManagementSystem.models.validateCompanyRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.companyRoutes() {
    val controller = CompanyController()
    val legacyAuthController = AuthController()

    fun io.ktor.server.request.ApplicationRequest.bearerToken(): String {
        val header = headers["Authorization"] ?: return ""
        if (!header.startsWith("Bearer ")) return ""
        return header.removePrefix("Bearer ").trim()
    }

    suspend fun io.ktor.server.routing.RoutingContext.requireCompanyAdmin(): Boolean {
        val bearer = call.request.bearerToken()
        val principal = ProviderRegistry.userContextProvider.getCurrentUser(bearer)
        if (principal != null) {
            val role = normalizeRole(principal.role)
            if (role == Role.SUPER_ADMIN || role == Role.COMPANY_ADMIN) return true
            call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden")); return false
        }

        val legacy = legacyAuthController.getValidatedSessionByToken(bearer)
        if (legacy != null) {
            val role = normalizeRole(legacy.role)
            if (role == Role.SUPER_ADMIN || role == Role.COMPANY_ADMIN) return true
            call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden")); return false
        }

        call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
        return false
    }

    route("/companies") {
        get("/kiosk") {
            try {
                val keyFromHeader = call.request.headers["X-Device-Key"]?.trim().orEmpty()
                val keyFromQuery = call.request.queryParameters["device_key"]?.trim().orEmpty()
                val deviceKey = keyFromHeader.ifBlank { keyFromQuery }

                if (deviceKey.isBlank()) {
                    return@get call.respond(HttpStatusCode.OK, SingleResponse(CompanyKioskBoard("QUEUING SYSTEM", controller.getActiveCompaniesForKiosk()), GlobalCredentialResponse(200, true, "OK")))
                }

                val device = ProviderRegistry.deviceAuthProvider.authenticateDevice(deviceKey, DeviceType.KIOSK.name)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Invalid kiosk device"))

                if (device.companyId != null) {
                    val scoped = controller.getActiveCompaniesForKiosk().filter { it.id == device.companyId }
                    return@get call.respond(HttpStatusCode.OK, SingleResponse(CompanyKioskBoard("QUEUING SYSTEM", scoped), GlobalCredentialResponse(200, true, "OK")))
                }

                call.respond(HttpStatusCode.OK, SingleResponse(CompanyKioskBoard("QUEUING SYSTEM", controller.getActiveCompaniesForKiosk()), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list") {
            try {
                if (!requireCompanyAdmin()) return@get
                call.respond(HttpStatusCode.OK, CompanyListResponse(controller.getCompanies(), GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/{id}") {
            try {
                if (!requireCompanyAdmin()) return@get
                val companyId = call.parameters["id"]?.toIntOrNull() ?: 0
                if (companyId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                val company = controller.getCompanyById(companyId)
                if (company.id <= 0) return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Company not found"))
                call.respond(HttpStatusCode.OK, CompanyResponse(company, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/create") {
            try {
                if (!requireCompanyAdmin()) return@post
                val request = call.receive<CompanyRequest>()
                val errors = request.validateCompanyRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                if (controller.getCompanyByCode(request.companyCode).exists) return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "companyCode must be unique"))
                val id = controller.postCompany(request)
                call.respond(HttpStatusCode.OK, IdResponse(id, GlobalCredentialResponse(200, id > 0, "Company created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update/{id}") {
            try {
                if (!requireCompanyAdmin()) return@put
                val companyId = call.parameters["id"]?.toIntOrNull() ?: 0
                if (companyId <= 0) return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                val request = call.receive<CompanyRequest>()
                val errors = request.validateCompanyRequest()
                if (errors.isNotEmpty()) return@put call.respond(HttpStatusCode.BadRequest, errors)
                if (controller.getCompanyByCodeExceptId(request.companyCode, companyId).exists) return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "companyCode must be unique"))
                val affected = controller.updateCompany(companyId, request)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Company updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        delete("/deactivate/{id}") {
            try {
                if (!requireCompanyAdmin()) return@delete
                val companyId = call.parameters["id"]?.toIntOrNull() ?: 0
                if (companyId <= 0) return@delete call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                val affected = controller.deactivateCompany(companyId)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Company deactivated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        delete("/{id}") {
            try {
                if (!requireCompanyAdmin()) return@delete
                val companyId = call.parameters["id"]?.toIntOrNull() ?: 0
                if (companyId <= 0) return@delete call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                val affected = controller.deleteCompany(companyId)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Company deleted"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, e.message ?: "Unable to delete company"))
            }
        }
    }
}
