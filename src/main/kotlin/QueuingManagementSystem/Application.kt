package QueuingManagementSystem

import io.ktor.server.application.Application
import QueuingManagementSystem.plugins.configureRouting
import QueuingManagementSystem.plugins.configureSerialization
import QueuingManagementSystem.plugins.configureSockets
import QueuingManagementSystem.config.UserSeeder

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureSockets()
    configureRouting()
    UserSeeder.seedUsers()
}
