package QueuingManagementSystem.auth.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthPrincipal(
    val userId: Int,
    val email: String,
    val fullName: String,
    val role: String,
    val companyId: Int?,
    val departmentId: Int?,
    val permissions: List<String> = emptyList(),
    val authSource: String
)
