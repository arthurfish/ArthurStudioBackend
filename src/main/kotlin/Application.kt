package io.github.arthurfish

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    val database = configureDatabases()
    val restEventStoreService = RestEventStoreService(database)
    restEventStoreService.initDatabase()
    configureSockets(restEventStoreService)
    configureRouting()
}
