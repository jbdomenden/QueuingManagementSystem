package QueuingManagementSystem

import io.ktor.server.application.Application
import QueuingManagementSystem.plugins.configureRouting
import QueuingManagementSystem.plugins.configureSerialization
import QueuingManagementSystem.plugins.configureSockets
import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.config.SampleUsersBootstrap
import QueuingManagementSystem.auth.services.AuthService
import QueuingManagementSystem.auth.services.JwtService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val postgresUrl = environment.config.property("postgres.url").getString()
    val postgresUser = environment.config.property("postgres.user").getString()
    val postgresPassword = environment.config.property("postgres.password").getString()
    ConnectionPoolManager.configure(postgresUrl, postgresUser, postgresPassword)

    configureSerialization()
    configureSockets()
    configureRouting()

    runCatching { SampleUsersBootstrap.bootstrap() }
        .onFailure { println("SampleUsersBootstrap skipped: ${it.message}") }

    val jwtService = JwtService(
        secret = environment.config.propertyOrNull("auth.jwt.secret")?.getString() ?: "change-me-secret",
        issuer = environment.config.propertyOrNull("auth.jwt.issuer")?.getString() ?: "qms",
        audience = environment.config.propertyOrNull("auth.jwt.audience")?.getString() ?: "qms-clients",
        expirationMinutes = environment.config.propertyOrNull("auth.jwt.expirationMinutes")?.getString()?.toLongOrNull() ?: 480L
    )
    runCatching { AuthService(jwtService = jwtService).bootstrapIfEmpty() }
        .onFailure { println("Staff auth bootstrap skipped: ${it.message}") }

}
