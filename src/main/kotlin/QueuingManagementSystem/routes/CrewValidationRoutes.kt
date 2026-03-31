package QueuingManagementSystem.routes

import QueuingManagementSystem.controllers.CrewValidationController
import QueuingManagementSystem.models.CrewValidationRequest
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.validateCrewValidationRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.crewValidationRoutes() {
    val controller = CrewValidationController()

    route("/crew-validation") {
        post("/validate") {
            try {
                val request = call.receive<CrewValidationRequest>()
                val errors = request.validateCrewValidationRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)

                val response = controller.validateCrewIdentifier(
                    companyIdParam = request.companyId,
                    companyTransactionIdParam = request.companyTransactionId,
                    identifierValueParam = request.identifierValue,
                    identifierTypeParam = request.identifierType
                )
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
