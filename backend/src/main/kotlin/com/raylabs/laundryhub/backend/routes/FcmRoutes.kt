package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.DeviceTokenRepository
import com.raylabs.laundryhub.core.domain.model.fcm.DeviceTokenRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.fcmRoutes(deviceTokenRepository: DeviceTokenRepository) {
    route("/api/notifications") {
        post("/token") {
            try {
                val request = call.receive<DeviceTokenRequest>()
                val success = deviceTokenRepository.registerToken(request.token)
                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("status" to "Error", "message" to "Failed to register token"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error", "message" to (e.message ?: "Invalid data")))
            }
        }
    }
}
