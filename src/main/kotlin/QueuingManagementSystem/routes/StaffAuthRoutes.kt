package QueuingManagementSystem.routes

import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.auth.models.StaffLoginRequest
import QueuingManagementSystem.auth.models.StaffChangePasswordRequest
import QueuingManagementSystem.config.ProviderRegistry
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.staffAuthRoutes() {
    fun io.ktor.server.request.ApplicationRequest.bearerToken(): String {
        val header = headers["Authorization"] ?: return ""
        if (!header.startsWith("Bearer ")) return ""
        return header.removePrefix("Bearer ").trim()
    }

    fun Route.installAuthApi(prefix: String) {
        route(prefix) {
            post("/login") {
                try {
                    val request = call.receive<StaffLoginRequest>()
                    if (request.email.isBlank() || request.password.isBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "email and password are required"))
                    }
                    val result = ProviderRegistry.authProvider.login(request.email, request.password)
                    val payload = mapOf(
                        "token" to result.token,
                        "forcePasswordChange" to result.forcePasswordChange,
                        "principal" to result.principal,
                        "result" to GlobalCredentialResponse(if (result.success) 200 else 401, result.success, result.message)
                    )
                    call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.Unauthorized, payload)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
                }
            }

            post("/change-password") {
                try {
                    val token = call.request.bearerToken()
                    if (token.isBlank()) return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

                    val request = call.receive<StaffChangePasswordRequest>()
                    if (request.currentPassword.isBlank() || request.newPassword.isBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "currentPassword and newPassword are required"))
                    }

                    val (ok, message) = ProviderRegistry.authProvider.changePassword(token, request.currentPassword, request.newPassword)
                    call.respond(if (ok) HttpStatusCode.OK else HttpStatusCode.BadRequest, GlobalCredentialResponse(if (ok) 200 else 400, ok, message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
                }
            }

            post("/logout") {
                val token = call.request.bearerToken()
                if (token.isBlank()) return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                val ok = ProviderRegistry.authProvider.logout(token)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, ok, if (ok) "Logged out" else "Logout failed"))
            }

            get("/me") {
                val token = call.request.bearerToken()
                if (token.isBlank()) return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

                val principal = ProviderRegistry.userContextProvider.getCurrentUser(token)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                call.respond(HttpStatusCode.OK, mapOf("principal" to principal, "result" to GlobalCredentialResponse(200, true, "OK")))
            }
        }
    }

    installAuthApi("/staff/auth")
    installAuthApi("/api/auth")
}
