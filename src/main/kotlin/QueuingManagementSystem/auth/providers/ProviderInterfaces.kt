package QueuingManagementSystem.auth.providers

import QueuingManagementSystem.auth.models.AuthPrincipal
import QueuingManagementSystem.devices.DeviceContext

data class AuthLoginResult(
    val token: String,
    val forcePasswordChange: Boolean,
    val principal: AuthPrincipal,
    val success: Boolean,
    val message: String
)

interface AuthProvider {
    fun login(email: String, password: String): AuthLoginResult
    fun changePassword(token: String, currentPassword: String, newPassword: String): Pair<Boolean, String>
    fun logout(token: String): Boolean
}

interface UserContextProvider {
    fun getCurrentUser(token: String): AuthPrincipal?
}

interface PermissionProvider {
    fun hasPermission(userId: Int, permissionCode: String): Boolean
}

interface DeviceAuthProvider {
    fun authenticateDevice(deviceKey: String, expectedType: String? = null): DeviceContext?
    fun resolveDeviceContext(deviceKey: String): DeviceContext?
}
