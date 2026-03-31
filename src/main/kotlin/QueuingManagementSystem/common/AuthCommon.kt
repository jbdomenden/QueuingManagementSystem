package QueuingManagementSystem.common

import io.ktor.server.request.ApplicationRequest
import QueuingManagementSystem.models.UserSessionModel

enum class UserRole {
    SUPERADMIN,
    DEPARTMENT_ADMIN,
    SUPERVISOR,
    MODERATOR,
    HANDLER
}

fun ApplicationRequest.extractBearerToken(): String {
    val authHeader = headers["Authorization"] ?: return ""
    if (!authHeader.startsWith("Bearer ")) return ""
    return authHeader.removePrefix("Bearer ").trim()
}

fun QueuingManagementSystem.models.UserSessionModel.hasDepartmentAccess(targetDepartmentId: Int): Boolean {
    return role == QueuingManagementSystem.common.UserRole.SUPERADMIN.name || department_id == targetDepartmentId
}
