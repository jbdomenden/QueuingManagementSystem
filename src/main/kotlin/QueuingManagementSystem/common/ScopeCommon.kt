package QueuingManagementSystem.common

import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.models.UserSessionModel
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

private val SUPER_ADMIN_ROLE_KEYS = setOf("SUPERADMIN", "SUPER_ADMIN", "super_admin")

fun AuthController.ValidatedSession.isSuperAdmin(): Boolean = role in SUPER_ADMIN_ROLE_KEYS
fun UserSessionModel.isSuperAdmin(): Boolean = role in SUPER_ADMIN_ROLE_KEYS

fun AuthController.ValidatedSession.allowedDepartmentIds(): Set<Int> {
    if (isSuperAdmin()) return emptySet()
    val scopes = departmentScopes.toMutableSet()
    if (departmentId != null) scopes.add(departmentId)
    return scopes
}

fun UserSessionModel.allowedDepartmentIds(): Set<Int> {
    if (isSuperAdmin()) return emptySet()
    val scopes = department_scopes.toMutableSet()
    if (department_id != null) scopes.add(department_id)
    return scopes
}

fun AuthController.ValidatedSession.canAccessDepartment(departmentId: Int): Boolean {
    if (isSuperAdmin()) return true
    return allowedDepartmentIds().contains(departmentId)
}

fun UserSessionModel.canAccessDepartment(departmentId: Int): Boolean {
    if (isSuperAdmin()) return true
    return allowedDepartmentIds().contains(departmentId)
}

suspend fun RoutingContext.requirePermissionAndDepartmentScope(
    session: AuthController.ValidatedSession,
    permission: String,
    departmentId: Int,
    scopeMessage: String = "Department scope violation"
): Boolean {
    if (!session.permissions.contains(permission)) {
        call.respond(HttpStatusCode.Forbidden, QueuingManagementSystem.models.GlobalCredentialResponse(403, false, "Missing permission: $permission"))
        return false
    }
    if (!session.canAccessDepartment(departmentId)) {
        call.respond(HttpStatusCode.Forbidden, QueuingManagementSystem.models.GlobalCredentialResponse(403, false, scopeMessage))
        return false
    }
    return true
}
