package QueuingManagementSystem.routes

import QueuingManagementSystem.common.clientIpAddress
import QueuingManagementSystem.common.clientUserAgent
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.common.requirePermission
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.authRoutes() {
    val config = application.environment.config
    val authController = AuthController(
        jwtSecret = config.propertyOrNull("auth.jwt.secret")?.getString() ?: "change-me-secret",
        jwtIssuer = config.propertyOrNull("auth.jwt.issuer")?.getString() ?: "qms",
        jwtAudience = config.propertyOrNull("auth.jwt.audience")?.getString() ?: "qms-clients",
        jwtExpirationMinutes = config.propertyOrNull("auth.jwt.expirationMinutes")?.getString()?.toLongOrNull() ?: 480L,
        singleSessionEnforced = config.propertyOrNull("auth.singleSessionEnforced")?.getString()?.toBooleanStrictOrNull() ?: false
    )

    route("/auth") {
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                val errors = request.validateLoginRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)

                val response = authController.login(
                    request.username,
                    request.password,
                    call.request.clientIpAddress(),
                    call.request.clientUserAgent(),
                    call.request.headers["X-Client-Id"]
                )
                if (!response.result.Access) return@post call.respond(HttpStatusCode.Unauthorized, response)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/logout") {
            try {
                val token = call.request.extractBearerToken()
                if (token.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Token is required"))
                val ok = authController.logout(token)
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, ok, if (ok) "Logged out" else "Session not active"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/validate") {
            try {
                val token = call.request.extractBearerToken()
                val response = authController.validateSession(token)
                if (!response.result.Access) return@get call.respond(HttpStatusCode.Unauthorized, response)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/change-password") {
            try {
                val token = call.request.extractBearerToken()
                if (token.isBlank()) return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
                val request = call.receive<ChangePasswordRequest>()
                val errors = request.validateChangePasswordRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)

                val result = authController.changePassword(token, request.currentPassword, request.newPassword, call.request.clientIpAddress(), call.request.clientUserAgent())
                call.respond(if (result.Access) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/me") {
            val token = call.request.extractBearerToken()
            val response = authController.validateSession(token)
            if (!response.result.Access) return@get call.respond(HttpStatusCode.Unauthorized, response)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
