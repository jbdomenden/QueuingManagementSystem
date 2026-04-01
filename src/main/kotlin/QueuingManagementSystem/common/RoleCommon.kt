package QueuingManagementSystem.common

import QueuingManagementSystem.auth.models.AuthPrincipal
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.models.UserSessionModel

enum class Role {
    SUPER_ADMIN,
    COMPANY_ADMIN,
    MANAGER,
    SUPERVISOR,
    ACCOUNTING,
    EMPLOYEE,
    DEPARTMENT_ADMIN,
    MODERATOR,
    HANDLER
}

private val ROLE_ALIASES: Map<String, Role> = mapOf(
    "SUPERADMIN" to Role.SUPER_ADMIN,
    "SUPER_ADMIN" to Role.SUPER_ADMIN,
    "COMPANYADMIN" to Role.COMPANY_ADMIN,
    "COMPANY_ADMIN" to Role.COMPANY_ADMIN,
    "MANAGER" to Role.MANAGER,
    "SUPERVISOR" to Role.SUPERVISOR,
    "ACCOUNTING" to Role.ACCOUNTING,
    "EMPLOYEE" to Role.EMPLOYEE,
    "DEPARTMENTADMIN" to Role.DEPARTMENT_ADMIN,
    "DEPARTMENT_ADMIN" to Role.DEPARTMENT_ADMIN,
    "MODERATOR" to Role.MODERATOR,
    "HANDLER" to Role.HANDLER
)

fun normalizeRole(rawRole: String?): Role? {
    if (rawRole.isNullOrBlank()) return null
    val normalized = rawRole.trim().uppercase()
    return ROLE_ALIASES[normalized] ?: ROLE_ALIASES[normalized.replace("_", "")]
}

fun UserSessionModel.normalizedRole(): Role? = normalizeRole(role)
fun AuthController.ValidatedSession.normalizedRole(): Role? = normalizeRole(role)
fun AuthPrincipal.normalizedRole(): Role? = normalizeRole(role)

fun UserSessionModel.canAccessCompany(companyId: Int?): Boolean = true
fun AuthController.ValidatedSession.canAccessCompany(companyId: Int?): Boolean = true
fun AuthPrincipal.canAccessCompany(companyId: Int?): Boolean {
    if (companyId == null) return true
    return this.companyId == null || this.companyId == companyId
}
