package marlow.systems.queuingsystem

import io.ktor.server.application.Application
import marlow.systems.queuingsystem.plugins.configureRouting
import marlow.systems.queuingsystem.plugins.configureSerialization
import marlow.systems.queuingsystem.plugins.configureSockets

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureSockets()
    configureRouting()
}
