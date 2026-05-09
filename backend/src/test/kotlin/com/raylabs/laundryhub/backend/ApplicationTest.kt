package com.raylabs.laundryhub.backend

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun testRootEndpoint() = testApplication {
        System.setProperty("isTest", "true")
        // No need to manually call application { ... } because EngineMain loads module() automatically
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("LaundryHub KMP Backend is running"))
    }

    @Test
    fun testSharedDTOEndpoint() = testApplication {
        System.setProperty("isTest", "true")
        val response = client.get("/api/test-shared")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val body = response.bodyAsText()
        // Verify that the Shared Module's DTO (OrderData) is serialized successfully to JSON
        assertTrue(body.contains("ORD-123"))
        assertTrue(body.contains("Cuci Komplit"))
        assertTrue(body.contains("John Doe"))
        assertTrue(body.contains("30000"))
    }

    @Test
    fun migrationRoutesAreDisabledByDefault() = testApplication {
        System.setProperty("isTest", "true")

        assertEquals(HttpStatusCode.NotFound, client.post("/api/migrate-orders").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/debug-sheets").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/debug-metadata").status)
        assertEquals(HttpStatusCode.NotFound, client.post("/api/packages/migrate").status)
        assertEquals(HttpStatusCode.NotFound, client.post("/api/outcomes/migrate").status)
        assertEquals(HttpStatusCode.NotFound, client.post("/api/gross/migrate").status)
        assertEquals(HttpStatusCode.NotFound, client.post("/api/summary/migrate").status)
    }
}
