package QueuingManagementSystem.routes

import QueuingManagementSystem.common.AccessKey
import QueuingManagementSystem.common.hasAccess
import QueuingManagementSystem.common.normalizeRole
import QueuingManagementSystem.common.Role
import QueuingManagementSystem.config.ProviderRegistry
import QueuingManagementSystem.controllers.AssetController
import QueuingManagementSystem.controllers.AuditController
import QueuingManagementSystem.controllers.StaffAccessController
import QueuingManagementSystem.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.staffAccessRoutes() {
    val users = StaffAccessController()
    val assets = AssetController()
    val audit = AuditController()

    fun io.ktor.server.request.ApplicationRequest.bearerToken(): String {
        val header = headers["Authorization"] ?: return ""
        if (!header.startsWith("Bearer ")) return ""
        return header.removePrefix("Bearer ").trim()
    }

    suspend fun RoutingContext.requireAccess(accessKey: AccessKey): QueuingManagementSystem.auth.models.AuthPrincipal? {
        val principal = ProviderRegistry.userContextProvider.getCurrentUser(call.request.bearerToken())
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized")); return null
            }
        if (!principal.hasAccess(accessKey)) {
            call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden")); return null
        }
        return principal
    }


    fun manageableRolesFor(role: String): Set<String> = when (role.uppercase()) {
        "SUPER_ADMIN" -> setOf("SUPER_ADMIN", "COMPANY_ADMIN", "MANAGER", "SUPERVISOR", "ACCOUNTING", "EMPLOYEE")
        "COMPANY_ADMIN" -> setOf("MANAGER", "SUPERVISOR", "ACCOUNTING", "EMPLOYEE")
        "MANAGER" -> setOf("SUPERVISOR", "ACCOUNTING", "EMPLOYEE")
        "SUPERVISOR" -> setOf("EMPLOYEE")
        else -> emptySet()
    }

    fun canManageRole(actorRole: String, targetRole: String): Boolean = manageableRolesFor(actorRole).contains(targetRole.uppercase())

    fun sameCompanyOrSuper(actor: QueuingManagementSystem.auth.models.AuthPrincipal, companyId: Int?): Boolean {
        if (normalizeRole(actor.role) == Role.SUPER_ADMIN) return true
        return actor.companyId != null && companyId != null && actor.companyId == companyId
    }

    route("/api/users") {
        get {
            requireAccess(AccessKey.USER_MANAGEMENT_VIEW) ?: return@get
            call.respond(ListResponse(users.listUsers(), GlobalCredentialResponse(200, true, "OK")))
        }
        get("/{id}") {
            requireAccess(AccessKey.USER_MANAGEMENT_VIEW) ?: return@get
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val user = users.getUserById(id) ?: return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Not found"))
            call.respond(SingleResponse(user, GlobalCredentialResponse(200, true, "OK")))
        }
        post {
            val actor = requireAccess(AccessKey.USER_MANAGEMENT_CREATE) ?: return@post
            val req = call.receive<UserCreateRequest>()
            if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(req.email)) return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid email"))
            if (!users.isValidRole(req.role)) return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid role"))
            if (!canManageRole(actor.role, req.role)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Role scope violation"))
            val scopedCompanyId = if (normalizeRole(actor.role) == Role.SUPER_ADMIN) req.companyId else actor.companyId
            if (!sameCompanyOrSuper(actor, scopedCompanyId)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Company scope violation"))
            if (users.emailExists(req.email)) return@post call.respond(HttpStatusCode.Conflict, GlobalCredentialResponse(409, false, "Email already exists"))
            val id = users.createUser(req.copy(companyId = scopedCompanyId))
            audit.createAuditLog(actor.userId, actor.departmentId, "USER_CREATED", "queue_users", id.toString(), "{}")
            call.respond(IdResponse(id, GlobalCredentialResponse(200, true, "User created")))
        }
        put("/{id}") {
            val actor = requireAccess(AccessKey.USER_MANAGEMENT_EDIT) ?: return@put
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val req = call.receive<UserUpdateRequest>()
            if (!users.isValidRole(req.role)) return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid role"))
            val target = users.getUserById(id) ?: return@put call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Not found"))
            if (!sameCompanyOrSuper(actor, target.companyId)) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Company scope violation"))
            if (!canManageRole(actor.role, target.role) || !canManageRole(actor.role, req.role)) return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Role scope violation"))
            val ok = users.updateUser(id, req.copy(companyId = if (normalizeRole(actor.role) == Role.SUPER_ADMIN) req.companyId else actor.companyId))
            if (ok) audit.createAuditLog(actor.userId, actor.departmentId, "USER_UPDATED", "queue_users", id.toString(), "{}")
            call.respond(GlobalCredentialResponse(if (ok) 200 else 404, ok, if (ok) "Updated" else "Not found"))
        }
        patch("/{id}/status") {
            val actor = requireAccess(AccessKey.USER_MANAGEMENT_EDIT) ?: return@patch
            val id = call.parameters["id"]?.toIntOrNull() ?: return@patch call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val req = call.receive<UserStatusPatchRequest>()
            val target = users.getUserById(id) ?: return@patch call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Not found"))
            if (!sameCompanyOrSuper(actor, target.companyId) || !canManageRole(actor.role, target.role)) return@patch call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Scope violation"))
            val ok = users.updateStatus(id, req.isActive)
            if (ok) audit.createAuditLog(actor.userId, actor.departmentId, if (req.isActive) "USER_ACTIVATED" else "USER_DEACTIVATED", "queue_users", id.toString(), "{}")
            call.respond(GlobalCredentialResponse(if (ok) 200 else 404, ok, if (ok) "Updated" else "Not found"))
        }
        patch("/{id}/role") {
            val actor = requireAccess(AccessKey.USER_MANAGEMENT_ASSIGN_ROLE) ?: return@patch
            val id = call.parameters["id"]?.toIntOrNull() ?: return@patch call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val req = call.receive<UserRolePatchRequest>()
            if (!users.isValidRole(req.role)) return@patch call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid role"))
            val target = users.getUserById(id) ?: return@patch call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Not found"))
            if (!sameCompanyOrSuper(actor, target.companyId) || !canManageRole(actor.role, target.role) || !canManageRole(actor.role, req.role)) return@patch call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Scope violation"))
            val ok = users.updateRole(id, req.role)
            if (ok) audit.createAuditLog(actor.userId, actor.departmentId, "USER_ROLE_CHANGED", "queue_users", id.toString(), "{}")
            call.respond(GlobalCredentialResponse(if (ok) 200 else 404, ok, if (ok) "Updated" else "Not found"))
        }
        patch("/{id}/access") {
            val actor = requireAccess(AccessKey.USER_MANAGEMENT_ASSIGN_ACCESS) ?: return@patch
            val id = call.parameters["id"]?.toIntOrNull() ?: return@patch call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val req = call.receive<UserAccessPatchRequest>()
            val target = users.getUserById(id) ?: return@patch call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Not found"))
            if (!sameCompanyOrSuper(actor, target.companyId) || !canManageRole(actor.role, target.role)) return@patch call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Scope violation"))
            val invalid = req.access.firstOrNull { !users.isValidAccessKey(it.accessKey) }
            if (invalid != null) return@patch call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid access key: ${invalid.accessKey}"))
            val ok = users.updateAccess(id, req.access)
            if (ok) audit.createAuditLog(actor.userId, actor.departmentId, "USER_ACCESS_CHANGED", "queue_users", id.toString(), "{}")
            call.respond(GlobalCredentialResponse(200, ok, if (ok) "Updated" else "Failed"))
        }
        post("/{id}/reset-password") {
            val actor = requireAccess(AccessKey.USER_MANAGEMENT_EDIT) ?: return@post
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val req = call.receive<ResetPasswordRequest>()
            val target = users.getUserById(id) ?: return@post call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Not found"))
            if (!sameCompanyOrSuper(actor, target.companyId) || !canManageRole(actor.role, target.role)) return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Scope violation"))
            val temp = users.resetPassword(id, req.password)
            audit.createAuditLog(actor.userId, actor.departmentId, "USER_PASSWORD_RESET", "queue_users", id.toString(), "{}")
            call.respond(ResetPasswordResponse(temp, GlobalCredentialResponse(200, true, "Password reset")))
        }
    }

    route("/api/assets") {
        get {
            requireAccess(AccessKey.ASSET_MANAGEMENT_VIEW) ?: return@get
            call.respond(ListResponse(assets.listAssets(), GlobalCredentialResponse(200, true, "OK")))
        }
        get("/{id}") {
            requireAccess(AccessKey.ASSET_MANAGEMENT_VIEW) ?: return@get
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val asset = assets.getAssetById(id) ?: return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Not found"))
            call.respond(SingleResponse(asset, GlobalCredentialResponse(200, true, "OK")))
        }
        post {
            val actor = requireAccess(AccessKey.ASSET_MANAGEMENT_CREATE) ?: return@post
            val req = call.receive<AssetRequest>()
            if (!assets.isValidType(req.assetType) || !assets.isValidStatus(req.status)) return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid asset type/status"))
            val id = assets.createAsset(req)
            audit.createAuditLog(actor.userId, actor.departmentId, "ASSET_CREATED", "assets", id.toString(), "{}")
            call.respond(IdResponse(id, GlobalCredentialResponse(200, true, "Asset created")))
        }
        put("/{id}") {
            val actor = requireAccess(AccessKey.ASSET_MANAGEMENT_EDIT) ?: return@put
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val req = call.receive<AssetRequest>()
            if (!assets.isValidType(req.assetType) || !assets.isValidStatus(req.status)) return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid asset type/status"))
            val ok = assets.updateAsset(id, req)
            if (ok) audit.createAuditLog(actor.userId, actor.departmentId, "ASSET_UPDATED", "assets", id.toString(), "{}")
            call.respond(GlobalCredentialResponse(if (ok) 200 else 404, ok, if (ok) "Updated" else "Not found"))
        }
        patch("/{id}/status") {
            val actor = requireAccess(AccessKey.ASSET_MANAGEMENT_EDIT) ?: return@patch
            val id = call.parameters["id"]?.toIntOrNull() ?: return@patch call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val req = call.receive<AssetStatusPatchRequest>()
            if (!assets.isValidStatus(req.status)) return@patch call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid status"))
            val ok = assets.updateStatus(id, req.status)
            if (ok) audit.createAuditLog(actor.userId, actor.departmentId, "ASSET_STATUS_CHANGED", "assets", id.toString(), "{}")
            call.respond(GlobalCredentialResponse(if (ok) 200 else 404, ok, if (ok) "Updated" else "Not found"))
        }
        delete("/{id}") {
            val actor = requireAccess(AccessKey.ASSET_MANAGEMENT_DELETE) ?: return@delete
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val ok = assets.deleteAsset(id)
            if (ok) audit.createAuditLog(actor.userId, actor.departmentId, "ASSET_DELETED", "assets", id.toString(), "{}")
            call.respond(GlobalCredentialResponse(if (ok) 200 else 404, ok, if (ok) "Deleted" else "Not found"))
        }
        post("/{id}/regenerate-device-key") {
            val actor = requireAccess(AccessKey.ASSET_MANAGEMENT_EDIT) ?: return@post
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Invalid id"))
            val key = assets.regenerateDeviceKey(id)
            audit.createAuditLog(actor.userId, actor.departmentId, "DEVICE_KEY_REGENERATED", "assets", id.toString(), "{}")
            call.respond(mapOf("deviceKey" to key, "result" to GlobalCredentialResponse(200, true, "Regenerated")))
        }
    }
}
