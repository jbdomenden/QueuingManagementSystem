package QueuingManagementSystem.common

import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.models.GlobalCredentialResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.requirePermission(authController: AuthController, permissionCode: String): Boolean {
    val token = call.request.extractBearerToken()
    val session = authController.getValidatedSessionByToken(token)
    if (session == null) {
        call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
        return false
    }
    if (!session.permissions.contains(permissionCode)) {
        call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
        return false
    }
    return true
}

fun ApplicationRequest.clientIpAddress(): String {
    return headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
        ?: headers["X-Real-IP"]
        ?: local.remoteHost
}

fun ApplicationRequest.clientUserAgent(): String {
    return headers["User-Agent"] ?: "unknown"
}
