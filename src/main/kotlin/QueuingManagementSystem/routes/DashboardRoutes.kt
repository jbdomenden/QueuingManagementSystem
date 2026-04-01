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
                DashboardModuleModel("USER_MANAGEMENT", "User Management", "/users.html", "users", principal.hasAccess(AccessKey.USER_MANAGEMENT_VIEW), principal.hasAccess(AccessKey.USER_MANAGEMENT_VIEW)),
                DashboardModuleModel("ASSET_MANAGEMENT", "Asset Management", "/assets.html", "assets", principal.hasAccess(AccessKey.ASSET_MANAGEMENT_VIEW), principal.hasAccess(AccessKey.ASSET_MANAGEMENT_VIEW)),
                DashboardModuleModel("COMPANIES", "Companies", "/companies.html", "companies", isSuperAdmin, isSuperAdmin),
                DashboardModuleModel("COMPANY_TRANSACTIONS", "Company Transactions", "/company-transactions.html", "transactions", isSuperAdmin, isSuperAdmin),
                DashboardModuleModel("TRANSACTION_DESTINATIONS", "Transaction Destinations", "/company-transaction-destinations.html", "destinations", isSuperAdmin, isSuperAdmin),
                DashboardModuleModel("DISPLAY_MANAGEMENT", "Display", "/display.html", "display", isSuperAdmin, isSuperAdmin),
                DashboardModuleModel("ARCHIVED", "Archived", "/archived.html", "archived", isSuperAdmin, isSuperAdmin)
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
