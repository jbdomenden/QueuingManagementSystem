package QueuingManagementSystem.common

import QueuingManagementSystem.auth.models.AuthPrincipal

enum class AccessKey {
    USER_MANAGEMENT_VIEW,
    USER_MANAGEMENT_CREATE,
    USER_MANAGEMENT_EDIT,
    USER_MANAGEMENT_ASSIGN_ROLE,
    USER_MANAGEMENT_ASSIGN_ACCESS,
    ASSET_MANAGEMENT_VIEW,
    ASSET_MANAGEMENT_CREATE,
    ASSET_MANAGEMENT_EDIT,
    ASSET_MANAGEMENT_DELETE,
    QUEUE_ADMIN_VIEW,
    REPORTS_VIEW,
    AUDIT_VIEW
}

val ALL_ACCESS_KEYS: Set<String> = AccessKey.entries.map { it.name }.toSet()

fun AuthPrincipal.hasAccess(accessKey: AccessKey): Boolean {
    if (normalizeRole(role) == Role.SUPER_ADMIN) return true
    return permissions.contains(accessKey.name)
}
