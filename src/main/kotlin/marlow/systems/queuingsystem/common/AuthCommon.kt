package marlow.systems.queuingsystem.common

import io.ktor.server.request.ApplicationRequest
import marlow.systems.queuingsystem.models.UserSessionModel

enum class UserRole {
    SUPERADMIN,
    DEPARTMENT_ADMIN,
    HANDLER
}

fun ApplicationRequest.extractBearerToken(): String {
    val authHeader = headers["Authorization"] ?: return ""
    if (!authHeader.startsWith("Bearer ")) return ""
    return authHeader.removePrefix("Bearer ").trim()
}

fun UserSessionModel.hasDepartmentAccess(targetDepartmentId: Int): Boolean {
    return role == UserRole.SUPERADMIN.name || department_id == targetDepartmentId
}
