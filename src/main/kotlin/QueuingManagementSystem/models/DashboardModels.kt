package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class DashboardModuleModel(
    val moduleKey: String,
    val moduleLabel: String,
    val route: String,
    val iconKey: String,
    val isVisible: Boolean,
    val isEnabled: Boolean
)

@Serializable
data class SuperAdminDashboardResponse(
    val modules: List<DashboardModuleModel>,
    val result: GlobalCredentialResponse
)
