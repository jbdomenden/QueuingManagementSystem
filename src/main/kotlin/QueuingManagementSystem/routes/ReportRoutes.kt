package QueuingManagementSystem.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import QueuingManagementSystem.controllers.ReportController
import QueuingManagementSystem.models.GlobalCredentialResponse

fun Route.reportRoutes() {
    val controller = ReportController()
    route("/reports") {
        get("/department-summary/{departmentId}") { val id = call.parameters["departmentId"]?.toIntOrNull() ?: 0; if (id <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "departmentId is required")); call.respond(HttpStatusCode.OK, controller.getDepartmentSummary(id)) }
        get("/handler-performance/{departmentId}") { val id = call.parameters["departmentId"]?.toIntOrNull() ?: 0; call.respond(HttpStatusCode.OK, controller.getHandlerPerformance(id)) }
        get("/queue-volume/{departmentId}") { val id = call.parameters["departmentId"]?.toIntOrNull() ?: 0; call.respond(HttpStatusCode.OK, controller.getQueueVolume(id)) }
    }
}
