package com.raylabs.laundryhub.backend

import com.raylabs.laundryhub.backend.plugins.configureRouting
import com.raylabs.laundryhub.backend.plugins.configureSerialization
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun testRootEndpoint() = testApplication {
        application {
            configureSerialization()
            // We intentionally skip configureDatabase() to prevent Postgres connection errors during isolated CI tests
            configureRouting()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("LaundryHub KMP Backend is running"))
    }

    @Test
    fun testSharedDTOEndpoint() = testApplication {
        application {
            configureSerialization()
            configureRouting()
        }
        val response = client.get("/api/test-shared")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val body = response.bodyAsText()
        // Verify that the Shared Module's DTO (OrderData) is serialized successfully to JSON
        assertTrue(body.contains("ORD-123"))
        assertTrue(body.contains("Cuci Komplit"))
        assertTrue(body.contains("John Doe"))
        assertTrue(body.contains("30000"))
    }
}
