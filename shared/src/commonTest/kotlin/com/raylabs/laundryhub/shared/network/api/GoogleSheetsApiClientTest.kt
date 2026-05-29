package com.raylabs.laundryhub.shared.network.api

import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateValuesRequest
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GoogleSheetsApiClientTest {

    @Test
    fun `BatchUpdateValuesRequest serializes required valueInputOption`() {
        val json = Json {
            ignoreUnknownKeys = true
        }.encodeToString(batchUpdateRequest())

        assertTrue(json.contains("\"valueInputOption\":\"USER_ENTERED\""))
    }

    @Test
    fun `batchUpdateValues sends valueInputOption in request body`() = runTest {
        var requestBody = ""
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.OK,
                content = """
                    {
                      "spreadsheetId": "sheet-id",
                      "totalUpdatedRows": 1,
                      "totalUpdatedCells": 12,
                      "responses": [
                        { "updatedRows": 1, "updatedCells": 12 }
                      ]
                    }
                """.trimIndent(),
                onBody = { requestBody = it }
            )
        )

        val response = client.batchUpdateValues(
            spreadsheetId = "sheet-id",
            request = batchUpdateRequest(),
            accessToken = "token"
        )

        assertEquals(1, response.totalUpdatedRows)
        assertTrue(requestBody.contains("\"valueInputOption\":\"USER_ENTERED\""))
    }

    @Test
    fun `batchUpdateValues exposes Google error body when request fails`() = runTest {
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.BadRequest,
                content = """
                    {
                      "error": {
                        "code": 400,
                        "message": "Invalid valueInputOption",
                        "status": "INVALID_ARGUMENT"
                      }
                    }
                """.trimIndent()
            )
        )

        val error = assertFailsWith<IllegalStateException> {
            client.batchUpdateValues(
                spreadsheetId = "sheet-id",
                request = batchUpdateRequest(),
                accessToken = "token"
            )
        }

        assertTrue(error.message.orEmpty().contains("Google Sheets API error 400"))
        assertTrue(error.message.orEmpty().contains("Invalid valueInputOption"))
    }

    private fun batchUpdateRequest() = BatchUpdateValuesRequest(
        valueInputOption = "USER_ENTERED",
        data = listOf(
            ValueRange(
                range = "income!A2:L2",
                majorDimension = "ROWS",
                values = listOf(listOf("1674", "29/05/2026"))
            )
        )
    )

    private fun mockClient(
        status: HttpStatusCode,
        content: String,
        onBody: (String) -> Unit = {}
    ) = HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            addHandler { request ->
                onBody((request.body as TextContent).text)
                respond(
                    content = content,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
    }
}
