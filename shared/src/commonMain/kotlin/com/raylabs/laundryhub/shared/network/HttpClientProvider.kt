package com.raylabs.laundryhub.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientProvider {

    fun createClient(enableLogging: Boolean = true): HttpClient {
        return HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 60000L // 60 seconds
                connectTimeoutMillis = 60000L
                socketTimeoutMillis = 60000L
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            if (enableLogging) {
                install(Logging) {
                    logger = Logger.SIMPLE
                    level = LogLevel.INFO
                }
            }
        }
    }
}
