package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.PackageRepository
import com.raylabs.laundryhub.backend.db.repository.SyncDeleteEventRepository
import com.raylabs.laundryhub.backend.service.SheetsPushScheduler
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackageRoutesTest {

    private val repository: PackageRepository = mock()
    private val sheetsApiClient: GoogleSheetsApiClient = mock()
    private val syncDeleteEventRepository: SyncDeleteEventRepository = mock()
    private val sheetsPushScheduler: SheetsPushScheduler = mock()

    @Test
    fun `get packages returns ok and repository packages`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        val packages = listOf(PackageData(name = "Express", price = "10000", duration = "1", unit = "kg"))
        whenever(repository.getAll()).thenReturn(packages)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = false,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.get("/api/packages")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Express"))
    }

    @Test
    fun `post package creates package successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.insert(any())).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = false,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.post("/api/packages") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Express","price":"10000","duration":"1","unit":"kg"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("Success"))
        verify(sheetsPushScheduler).requestPush("package created")
    }

    @Test
    fun `put package updates package successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.update(eq("Express"), any())).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = false,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.put("/api/packages/Express") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Premium Express","price":"12000","duration":"1","unit":"kg"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Success"))
        verify(sheetsPushScheduler).requestPush("package updated")
    }

    @Test
    fun `delete package deletes package successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.delete("Express")).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = false,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.delete("/api/packages/Express")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("queued"))
        verify(sheetsPushScheduler).requestPush("package deleted")
    }

    @Test
    fun `post package returns Conflict when insertion fails`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.insert(any())).thenReturn(false)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = false,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.post("/api/packages") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Express","price":"10000","duration":"1","unit":"kg"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `post package returns BadRequest on invalid body`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = false,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.post("/api/packages") {
            contentType(ContentType.Application.Json)
            setBody("invalid-json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `put package returns NotFound when update fails`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.update(eq("Express"), any())).thenReturn(false)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = false,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.put("/api/packages/Express") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Express","price":"10000","duration":"1","unit":"kg"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `put package returns BadRequest on invalid body`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = false,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.put("/api/packages/Express") {
            contentType(ContentType.Application.Json)
            setBody("invalid-json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `delete package returns NotFound when delete fails`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.delete("Express")).thenReturn(false)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = false,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.delete("/api/packages/Express")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `migrate packages returns BadRequest when spreadsheetId is missing`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = true,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.post("/api/packages/migrate?accessToken=token")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `migrate packages returns InternalServerError when api client fails`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(sheetsApiClient.getValues(any(), any(), any())).thenThrow(RuntimeException("API Error"))

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                packageRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    migrationRoutesEnabled = true,
                    syncDeleteEventRepository = syncDeleteEventRepository,
                    sheetsPushScheduler = sheetsPushScheduler
                )
            }
        }

        val response = client.post("/api/packages/migrate?spreadsheetId=sid&accessToken=token")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("API Error", response.bodyAsText())
    }
}
