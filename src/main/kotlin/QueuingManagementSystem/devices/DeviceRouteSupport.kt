package QueuingManagementSystem.devices

import QueuingManagementSystem.config.ProviderRegistry
import QueuingManagementSystem.models.GlobalCredentialResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.requireDeviceContext(expectedType: DeviceType): DeviceContext? {
    val keyFromHeader = call.request.headers["X-Device-Key"]?.trim().orEmpty()
    val keyFromQuery = call.request.queryParameters["device_key"]?.trim().orEmpty()
    val deviceKey = keyFromHeader.ifBlank { keyFromQuery }
    if (deviceKey.isBlank()) {
        call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "device_key is required"))
        return null
    }

    val context = ProviderRegistry.deviceAuthProvider.authenticateDevice(deviceKey, expectedType.name)
    if (context == null) {
        call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Invalid device"))
        return null
    }
    return context
}
