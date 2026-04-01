package QueuingManagementSystem.config

import QueuingManagementSystem.auth.providers.AuthProvider
import QueuingManagementSystem.auth.providers.DeviceAuthProvider
import QueuingManagementSystem.auth.providers.PermissionProvider
import QueuingManagementSystem.auth.providers.UserContextProvider

object ProviderRegistry {
    lateinit var authProvider: AuthProvider
    lateinit var userContextProvider: UserContextProvider
    lateinit var permissionProvider: PermissionProvider
    lateinit var deviceAuthProvider: DeviceAuthProvider
}
