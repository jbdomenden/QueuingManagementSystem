package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.models.validateLoginRequest
import marlow.systems.queuingsystem.models.*

fun Route.authRoutes() {
    val authController = _root_ide_package_.QueuingManagementSystem.controllers.AuthController()
    route("/auth") {
        post("/login") {
            try {
                val request = call.receive<QueuingManagementSystem.models.LoginRequest>()
                val errors = request.validateLoginRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                val response = authController.login(request.username, request.password)
                if (!response.result.Access) return@post call.respond(HttpStatusCode.Unauthorized, response)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                        500,
                        false,
                        e.message ?: "Internal server error"
                    )
                )
            }
        }

        get("/me") {
            try {
                val token = call.request.extractBearerToken()
                val session = authController.getUserSessionByToken(token)
                if (session.user_id <= 0) return@get call.respond(HttpStatusCode.Unauthorized,
                    _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                        401,
                        false,
                        "Unauthorized"
                    )
                )
                call.respond(HttpStatusCode.OK, session)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                        500,
                        false,
                        e.message ?: "Internal server error"
                    )
                )
            }
        }
    }
}
