package QueuingManagementSystem.routes

import QueuingManagementSystem.auth.services.AuthService
import QueuingManagementSystem.auth.services.JwtService
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.auth.models.StaffLoginRequest
import QueuingManagementSystem.auth.models.StaffChangePasswordRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.staffAuthRoutes() {
    val config = application.environment.config
    val jwtService = JwtService(
        secret = config.propertyOrNull("auth.jwt.secret")?.getString() ?: "change-me-secret",
        issuer = config.propertyOrNull("auth.jwt.issuer")?.getString() ?: "qms",
        audience = config.propertyOrNull("auth.jwt.audience")?.getString() ?: "qms-clients",
        expirationMinutes = config.propertyOrNull("auth.jwt.expirationMinutes")?.getString()?.toLongOrNull() ?: 480L
    )
    val authService = AuthService(jwtService = jwtService)

    fun io.ktor.server.request.ApplicationRequest.bearerToken(): String {
        val header = headers["Authorization"] ?: return ""
        if (!header.startsWith("Bearer ")) return ""
        return header.removePrefix("Bearer ").trim()
    }

    route("/staff/auth") {
        post("/login") {
            try {
                val request = call.receive<StaffLoginRequest>()
                if (request.email.isBlank() || request.password.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "email and password are required"))
                }
                val result = authService.login(request.email, request.password)
                call.respond(if (result.result.Access) HttpStatusCode.OK else HttpStatusCode.Unauthorized, result)
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

                val result = authService.changePassword(token, request.currentPassword, request.newPassword)
                call.respond(if (result.Access) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/me") {
            val token = call.request.bearerToken()
            if (token.isBlank()) return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

            val result = authService.getCurrentUser(token)
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            call.respond(HttpStatusCode.OK, result)
        }
    }
}
