package QueuingManagementSystem.common

import io.ktor.server.request.ApplicationRequest
import QueuingManagementSystem.models.UserSessionModel

fun ApplicationRequest.extractBearerToken(): String {
    val authHeader = headers["Authorization"] ?: return ""
    if (!authHeader.startsWith("Bearer ")) return ""
    return authHeader.removePrefix("Bearer ").trim()
}

fun UserSessionModel.hasDepartmentAccess(targetDepartmentId: Int): Boolean {
    return canAccessDepartment(targetDepartmentId)
}
