package QueuingManagementSystem.auth.providers

import QueuingManagementSystem.auth.services.AuthService
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.devices.DeviceAuthService
import QueuingManagementSystem.devices.DeviceType
import QueuingManagementSystem.models.GlobalCredentialResponse

class LocalAuthProvider(private val authService: AuthService) : AuthProvider {
    override fun login(email: String, password: String): AuthLoginResult {
        val result = authService.login(email, password)
        return AuthLoginResult(
            token = result.accessToken,
            forcePasswordChange = result.forcePasswordChange,
            principal = result.principal,
            success = result.result.Access,
            message = result.result.Status
        )
    }

    override fun changePassword(token: String, currentPassword: String, newPassword: String): Pair<Boolean, String> {
        val result: GlobalCredentialResponse = authService.changePassword(token, currentPassword, newPassword)
        return Pair(result.Access, result.Status)
    }

    override fun logout(token: String): Boolean = token.isNotBlank()
}

class LocalUserContextProvider(private val authService: AuthService) : UserContextProvider {
    override fun getCurrentUser(token: String) = authService.getCurrentUser(token)?.principal
}

class LocalPermissionProvider(private val authController: AuthController = AuthController()) : PermissionProvider {
    override fun hasPermission(userId: Int, permissionCode: String): Boolean = true
}

class LocalDeviceAuthProvider(private val service: DeviceAuthService = DeviceAuthService()) : DeviceAuthProvider {
    override fun authenticateDevice(deviceKey: String, expectedType: String?) =
        service.authenticateDevice(deviceKey, expectedType?.let { DeviceType.valueOf(it) })

    override fun resolveDeviceContext(deviceKey: String) = service.resolveDeviceContext(deviceKey)
}
