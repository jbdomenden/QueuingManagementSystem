package QueuingManagementSystem

import io.ktor.server.application.Application
import QueuingManagementSystem.plugins.configureRouting
import QueuingManagementSystem.plugins.configureSerialization
import QueuingManagementSystem.plugins.configureSockets
import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.config.SampleUsersBootstrap
import QueuingManagementSystem.auth.services.AuthService
import QueuingManagementSystem.auth.services.JwtService
import QueuingManagementSystem.auth.providers.LocalAuthProvider
import QueuingManagementSystem.auth.providers.LocalDeviceAuthProvider
import QueuingManagementSystem.auth.providers.LocalPermissionProvider
import QueuingManagementSystem.auth.providers.LocalUserContextProvider
import QueuingManagementSystem.config.ProviderRegistry

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val postgresUrl = environment.config.property("postgres.url").getString()
    val postgresUser = environment.config.property("postgres.user").getString()
    val postgresPassword = environment.config.property("postgres.password").getString()
    ConnectionPoolManager.configure(postgresUrl, postgresUser, postgresPassword)

    val appMode = System.getenv("APP_MODE") ?: "local"
    val authProviderKey = (System.getenv("AUTH_PROVIDER") ?: "local").lowercase()
    val jwtSecret = System.getenv("JWT_SECRET") ?: environment.config.propertyOrNull("auth.jwt.secret")?.getString() ?: "change-me-secret"
    val jwtExpiration = (System.getenv("JWT_EXPIRATION") ?: environment.config.propertyOrNull("auth.jwt.expirationMinutes")?.getString() ?: "480").toLongOrNull() ?: 480L
    val jwtIssuer = environment.config.propertyOrNull("auth.jwt.issuer")?.getString() ?: "qms"
    val jwtAudience = environment.config.propertyOrNull("auth.jwt.audience")?.getString() ?: "qms-clients"

    if (authProviderKey != "local") {
        error("Unsupported AUTH_PROVIDER='$authProviderKey'. Supported providers: local")
    }

    val jwtService = JwtService(jwtSecret, jwtIssuer, jwtAudience, jwtExpiration)
    val authService = AuthService(jwtService = jwtService)

    ProviderRegistry.authProvider = LocalAuthProvider(authService)
    ProviderRegistry.userContextProvider = LocalUserContextProvider(authService)
    ProviderRegistry.permissionProvider = LocalPermissionProvider()
    ProviderRegistry.deviceAuthProvider = LocalDeviceAuthProvider()

    configureSerialization()
    configureSockets()
    configureRouting()

    runCatching { SampleUsersBootstrap.bootstrap() }
        .onFailure { println("SampleUsersBootstrap skipped: ${it.message}") }

    runCatching { authService.bootstrapIfEmpty() }
        .onFailure { println("Staff auth bootstrap skipped: ${it.message}") }

    println("QMS started in APP_MODE=$appMode AUTH_PROVIDER=$authProviderKey")
}
