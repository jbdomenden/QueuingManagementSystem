package QueuingManagementSystem.routes

import QueuingManagementSystem.common.AccessKey
import QueuingManagementSystem.common.hasAccess
import QueuingManagementSystem.common.normalizeRole
import QueuingManagementSystem.common.Role
import QueuingManagementSystem.config.ProviderRegistry
import QueuingManagementSystem.models.DashboardModuleModel
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.SuperAdminDashboardResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.dashboardRoutes() {
    fun io.ktor.server.request.ApplicationRequest.bearerToken(): String {
        val header = headers["Authorization"] ?: return ""
        if (!header.startsWith("Bearer ")) return ""
        return header.removePrefix("Bearer ").trim()
    }

    route("/api/dashboard") {
        get("/superadmin") {
            val principal = ProviderRegistry.userContextProvider.getCurrentUser(call.request.bearerToken())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))

            val isSuperAdmin = normalizeRole(principal.role) == Role.SUPER_ADMIN
            val canDashboardView = isSuperAdmin || principal.hasAccess(AccessKey.USER_MANAGEMENT_VIEW) || principal.hasAccess(AccessKey.ASSET_MANAGEMENT_VIEW)
            if (!canDashboardView) {
                return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
            }

            val modules = listOf(
                DashboardModuleModel(
                    moduleKey = "USER_MANAGEMENT",
                    moduleLabel = "User Management",
                    route = "/users.html",
                    iconKey = "users",
                    isVisible = principal.hasAccess(AccessKey.USER_MANAGEMENT_VIEW),
                    isEnabled = principal.hasAccess(AccessKey.USER_MANAGEMENT_VIEW)
                ),
                DashboardModuleModel(
                    moduleKey = "ASSET_MANAGEMENT",
                    moduleLabel = "Asset Management",
                    route = "/assets.html",
                    iconKey = "assets",
                    isVisible = principal.hasAccess(AccessKey.ASSET_MANAGEMENT_VIEW),
                    isEnabled = principal.hasAccess(AccessKey.ASSET_MANAGEMENT_VIEW)
                )
            )

            call.respond(
                HttpStatusCode.OK,
                SuperAdminDashboardResponse(
                    modules = modules,
                    result = GlobalCredentialResponse(200, true, "OK")
                )
            )
        }
    }
}
