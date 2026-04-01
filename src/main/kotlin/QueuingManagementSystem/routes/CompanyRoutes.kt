package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.devices.requireDeviceContext
import QueuingManagementSystem.devices.DeviceType
import QueuingManagementSystem.common.Role
import QueuingManagementSystem.common.requireAnyRole
import QueuingManagementSystem.common.requireRole
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.CompanyController
import QueuingManagementSystem.models.CompanyKioskBoard
import QueuingManagementSystem.models.CompanyRequest
import QueuingManagementSystem.models.CompanyResponse
import QueuingManagementSystem.models.CompanyListResponse
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
    val authController = AuthController()
    val controller = CompanyController()

    route("/companies") {
        get("/kiosk") {
            try {
                val device = requireDeviceContext(DeviceType.KIOSK) ?: return@get
                if (device.companyId != null) {
                    val scoped = controller.getActiveCompaniesForKiosk().filter { it.id == device.companyId }
                    return@get call.respond(HttpStatusCode.OK, SingleResponse(CompanyKioskBoard("QUEUING SYSTEM", scoped), GlobalCredentialResponse(200, true, "OK")))
                }
                call.respond(
                    HttpStatusCode.OK,
                    SingleResponse(
                        CompanyKioskBoard("QUEUING SYSTEM", controller.getActiveCompaniesForKiosk()),
                        GlobalCredentialResponse(200, true, "OK")
                    )
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireRole(session.role, Role.SUPER_ADMIN)) return@get

                call.respond(
                    HttpStatusCode.OK,
                    CompanyListResponse(controller.getCompanies(), GlobalCredentialResponse(200, true, "OK"))
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/{id}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireRole(session.role, Role.SUPER_ADMIN)) return@get

                val companyId = call.parameters["id"]?.toIntOrNull() ?: 0
                if (companyId <= 0) {
                    return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                }

                val company = controller.getCompanyById(companyId)
                if (company.id <= 0) {
                    return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Company not found"))
                }

                call.respond(
                    HttpStatusCode.OK,
                    CompanyResponse(company, GlobalCredentialResponse(200, true, "OK"))
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/create") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireRole(session.role, Role.SUPER_ADMIN)) return@post

                val request = call.receive<CompanyRequest>()
                val errors = request.validateCompanyRequest()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.BadRequest, errors)
                }

                if (controller.getCompanyByCode(request.companyCode).exists) {
                    return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "companyCode must be unique"))
                }

                val id = controller.postCompany(request)
                call.respond(HttpStatusCode.OK, IdResponse(id, GlobalCredentialResponse(200, id > 0, "Company created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update/{id}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireRole(session.role, Role.SUPER_ADMIN)) return@put

                val companyId = call.parameters["id"]?.toIntOrNull() ?: 0
                if (companyId <= 0) {
                    return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                }

                val request = call.receive<CompanyRequest>()
                val errors = request.validateCompanyRequest()
                if (errors.isNotEmpty()) {
                    return@put call.respond(HttpStatusCode.BadRequest, errors)
                }

                if (controller.getCompanyByCodeExceptId(request.companyCode, companyId).exists) {
                    return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "companyCode must be unique"))
                }

                val affected = controller.updateCompany(companyId, request)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Company updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        delete("/deactivate/{id}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireRole(session.role, Role.SUPER_ADMIN)) return@delete

                val companyId = call.parameters["id"]?.toIntOrNull() ?: 0
                if (companyId <= 0) {
                    return@delete call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                }

                val affected = controller.deactivateCompany(companyId)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Company deactivated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        delete("/{id}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireRole(session.role, Role.SUPER_ADMIN)) return@delete

                val companyId = call.parameters["id"]?.toIntOrNull() ?: 0
                if (companyId <= 0) {
                    return@delete call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
                }

                val affected = controller.deleteCompany(companyId)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Company deleted"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, e.message ?: "Unable to delete company"))
            }
        }
    }
}
