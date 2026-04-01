package QueuingManagementSystem.auth.models

import QueuingManagementSystem.models.GlobalCredentialResponse
import kotlinx.serialization.Serializable

@Serializable
data class StaffLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class StaffChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class StaffLoginResult(
    val accessToken: String,
    val forcePasswordChange: Boolean,
    val principal: AuthPrincipal,
    val result: GlobalCredentialResponse
)

@Serializable
data class StaffCurrentUserResult(
    val principal: AuthPrincipal,
    val forcePasswordChange: Boolean,
    val result: GlobalCredentialResponse
)
