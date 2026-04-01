package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.common.Role
import QueuingManagementSystem.common.requireAnyRole
import QueuingManagementSystem.common.requireRole
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.CompanyTransactionController
import QueuingManagementSystem.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.companyTransactionRoutes() {
    val authController = AuthController()
    val controller = CompanyTransactionController()

    route("/company-transactions") {
        get("/kiosk/company/{companyId}") {
            try {
                val companyId = call.parameters["companyId"]?.toIntOrNull() ?: 0
                if (companyId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "companyId is required"))

                call.respond(
                    HttpStatusCode.OK,
                    CompanyTransactionKioskListResponse(
                        controller.getActiveCompanyTransactionsForKiosk(companyId),
                        GlobalCredentialResponse(200, true, "OK")
                    )
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/company/{companyId}") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@get

                val companyId = call.parameters["companyId"]?.toIntOrNull() ?: 0
                if (companyId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "companyId is required"))

                call.respond(
                    HttpStatusCode.OK,
                    CompanyTransactionListResponse(
                        controller.getCompanyTransactionsByCompanyId(companyId),
                        GlobalCredentialResponse(200, true, "OK")
                    )
                )
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

                val item = controller.getCompanyTransactionById(id)
                if (item.id <= 0) return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Company transaction not found"))

                call.respond(HttpStatusCode.OK, CompanyTransactionResponse(item, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/create") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@post

                val request = call.receive<CompanyTransactionRequest>()
                val errors = request.validateCompanyTransactionRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)

                val id = controller.postCompanyTransaction(request)
                call.respond(HttpStatusCode.OK, IdResponse(id, GlobalCredentialResponse(200, id > 0, "Company transaction created")))
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

                val request = call.receive<CompanyTransactionRequest>()
                val errors = request.validateCompanyTransactionRequest()
                if (errors.isNotEmpty()) return@put call.respond(HttpStatusCode.BadRequest, errors)

                val affected = controller.updateCompanyTransaction(id, request)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Company transaction updated"))
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

                val request = call.receive<CompanyTransactionToggleRequest>()
                val errors = request.validateCompanyTransactionToggleRequest()
                if (errors.isNotEmpty()) return@patch call.respond(HttpStatusCode.BadRequest, errors)

                val affected = controller.toggleCompanyTransactionStatus(id, request.status)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Company transaction status updated"))
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

                val affected = controller.deactivateCompanyTransaction(id)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, affected > 0, "Company transaction deactivated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
