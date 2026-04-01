package QueuingManagementSystem.common

import QueuingManagementSystem.auth.models.AuthPrincipal
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.models.UserSessionModel
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

fun AuthController.ValidatedSession.isSuperAdmin(): Boolean = normalizedRole() == Role.SUPER_ADMIN
fun UserSessionModel.isSuperAdmin(): Boolean = normalizedRole() == Role.SUPER_ADMIN
fun AuthPrincipal.isSuperAdmin(): Boolean = normalizedRole() == Role.SUPER_ADMIN

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

fun AuthPrincipal.allowedDepartmentIds(): Set<Int> {
    if (isSuperAdmin()) return emptySet()
    return departmentId?.let { setOf(it) } ?: emptySet()
}

fun AuthController.ValidatedSession.canAccessDepartment(departmentId: Int): Boolean {
    if (isSuperAdmin()) return true
    return allowedDepartmentIds().contains(departmentId)
}

fun UserSessionModel.canAccessDepartment(departmentId: Int): Boolean {
    if (isSuperAdmin()) return true
    return allowedDepartmentIds().contains(departmentId)
}

fun AuthPrincipal.canAccessDepartment(departmentId: Int): Boolean {
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


suspend fun RoutingContext.requireDepartmentScope(session: UserSessionModel, departmentId: Int, message: String = "Department scope violation"): Boolean {
    if (session.canAccessDepartment(departmentId)) return true
    call.respond(HttpStatusCode.Forbidden, QueuingManagementSystem.models.GlobalCredentialResponse(403, false, message))
    return false
}

suspend fun RoutingContext.requireDepartmentScope(session: AuthController.ValidatedSession, departmentId: Int, message: String = "Department scope violation"): Boolean {
    if (session.canAccessDepartment(departmentId)) return true
    call.respond(HttpStatusCode.Forbidden, QueuingManagementSystem.models.GlobalCredentialResponse(403, false, message))
    return false
}

suspend fun RoutingContext.requireCompanyScope(principal: AuthPrincipal, companyId: Int?, message: String = "Company scope violation"): Boolean {
    if (principal.canAccessCompany(companyId)) return true
    call.respond(HttpStatusCode.Forbidden, QueuingManagementSystem.models.GlobalCredentialResponse(403, false, message))
    return false
}
