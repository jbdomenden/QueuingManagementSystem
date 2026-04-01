package QueuingManagementSystem.common

import QueuingManagementSystem.models.GlobalCredentialResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.requireRole(rawRole: String?, role: Role): Boolean {
    val normalized = normalizeRole(rawRole)
    if (normalized == role) return true
    call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
    return false
}

suspend fun RoutingContext.requireAnyRole(rawRole: String?, roles: Set<Role>): Boolean {
    val normalized = normalizeRole(rawRole)
    if (normalized != null && roles.contains(normalized)) return true
    call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
    return false
}
