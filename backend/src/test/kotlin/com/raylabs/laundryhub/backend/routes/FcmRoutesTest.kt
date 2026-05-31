package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.DeviceTokenRepository
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FcmRoutesTest {

    private val deviceTokenRepository: DeviceTokenRepository = mock()

    @Test
    fun `post token registers token successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(deviceTokenRepository.registerToken("fcm-token-123")).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                fcmRoutes(deviceTokenRepository)
            }
        }

        val response = client.post("/api/notifications/token") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"fcm-token-123"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Success"))
    }

    @Test
    fun `post token returns bad request on empty token`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                fcmRoutes(deviceTokenRepository)
            }
        }

        val response = client.post("/api/notifications/token") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"   "}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("required"))
    }
}
