package com.raylabs.laundryhub.backend

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.Before
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
}
