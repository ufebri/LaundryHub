package com.raylabs.laundryhub.backend

import com.raylabs.laundryhub.backend.plugins.configureDatabase
import com.raylabs.laundryhub.backend.plugins.configureRouting
import com.raylabs.laundryhub.backend.plugins.configureSerialization
import io.ktor.server.application.Application

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureDatabase()
    configureRouting()
}
