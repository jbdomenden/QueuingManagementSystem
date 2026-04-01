package QueuingManagementSystem

import QueuingManagementSystem.auth.providers.LocalAuthProvider
import QueuingManagementSystem.auth.providers.LocalDeviceAuthProvider
import QueuingManagementSystem.auth.providers.LocalPermissionProvider
import QueuingManagementSystem.auth.providers.LocalUserContextProvider
import QueuingManagementSystem.auth.services.AuthService
import QueuingManagementSystem.auth.services.JwtService
import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.config.LocalDatabaseInitializer
import QueuingManagementSystem.config.ProviderRegistry
import QueuingManagementSystem.config.SampleUsersBootstrap
import QueuingManagementSystem.plugins.configureRouting
import QueuingManagementSystem.plugins.configureSerialization
import QueuingManagementSystem.plugins.configureSockets
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

private val appLogger = LoggerFactory.getLogger("QueuingManagementSystem.Application")

fun Application.module() {
    val appMode = (System.getenv("APP_MODE") ?: "local").uppercase()
    val authProviderKey = (System.getenv("AUTH_PROVIDER") ?: "local").uppercase()

    val postgresUrl = environment.config.property("postgres.url").getString()
    val postgresUser = environment.config.property("postgres.user").getString()
    val postgresPassword = environment.config.property("postgres.password").getString()

    ConnectionPoolManager.configure(postgresUrl, postgresUser, postgresPassword)

    val isLocalModeWithLocalAuth = appMode == "LOCAL" && authProviderKey == "LOCAL"
    if (isLocalModeWithLocalAuth) {
        appLogger.info("Local mode detected (APP_MODE={} AUTH_PROVIDER={})", appMode, authProviderKey)
        LocalDatabaseInitializer.initialize()
        SampleUsersBootstrap.bootstrap()
    }

    if (authProviderKey != "LOCAL") {
        error("Unsupported AUTH_PROVIDER='$authProviderKey'. Supported providers: LOCAL")
    }

    val jwtSecret =
        System.getenv("JWT_SECRET") ?: environment.config.propertyOrNull("auth.jwt.secret")?.getString() ?: "change-me-secret"
    val jwtExpiration =
        (System.getenv("JWT_EXPIRATION") ?: environment.config.propertyOrNull("auth.jwt.expirationMinutes")?.getString() ?: "480")
            .toLongOrNull() ?: 480L
    val jwtIssuer = environment.config.propertyOrNull("auth.jwt.issuer")?.getString() ?: "qms"
    val jwtAudience = environment.config.propertyOrNull("auth.jwt.audience")?.getString() ?: "qms-clients"

    val jwtService = JwtService(jwtSecret, jwtIssuer, jwtAudience, jwtExpiration)
    val authService = AuthService(jwtService = jwtService)

    ProviderRegistry.authProvider = LocalAuthProvider(authService)
    ProviderRegistry.userContextProvider = LocalUserContextProvider(authService)
    ProviderRegistry.permissionProvider = LocalPermissionProvider()
    ProviderRegistry.deviceAuthProvider = LocalDeviceAuthProvider()

    configureSerialization()
    configureSockets()
    configureRouting()

    appLogger.info("QMS started in APP_MODE={} AUTH_PROVIDER={}", appMode, authProviderKey)
}
