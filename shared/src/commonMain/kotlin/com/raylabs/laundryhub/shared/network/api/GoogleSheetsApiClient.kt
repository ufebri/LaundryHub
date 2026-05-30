package com.raylabs.laundryhub.shared.network.api

import com.raylabs.laundryhub.shared.network.model.sheets.AppendValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.BatchClearValuesRequest
import com.raylabs.laundryhub.shared.network.model.sheets.BatchClearValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateSpreadsheetRequest
import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateSpreadsheetResponse
import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateValuesRequest
import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.ClearValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.UpdateValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GoogleSheetsApiClient(
    val httpClient: HttpClient
) {
    private val baseUrl = "https://sheets.googleapis.com/v4/spreadsheets"

    suspend fun getSpreadsheet(spreadsheetId: String, accessToken: String): String {
        val response = httpClient.get("$baseUrl/$spreadsheetId") {
            bearerAuth(accessToken)
        }
        return response.ensureGoogleSheetsSuccess().bodyAsText()
    }

    suspend fun getValues(
        spreadsheetId: String,
        range: String,
        accessToken: String
    ): ValueRange {
        return httpClient.get("$baseUrl/$spreadsheetId/values/$range") {
            bearerAuth(accessToken)
        }.googleSheetsBody()
    }

    suspend fun updateValues(
        spreadsheetId: String,
        range: String,
        valueRange: ValueRange,
        accessToken: String
    ): UpdateValuesResponse {
        return httpClient.put("$baseUrl/$spreadsheetId/values/$range?valueInputOption=USER_ENTERED") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(valueRange)
        }.googleSheetsBody()
    }

    suspend fun appendValues(
        spreadsheetId: String,
        range: String,
        valueRange: ValueRange,
        accessToken: String
    ): AppendValuesResponse {
        return httpClient.post("$baseUrl/$spreadsheetId/values/$range:append?valueInputOption=USER_ENTERED") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(valueRange)
        }.googleSheetsBody()
    }

    suspend fun clearValues(
        spreadsheetId: String,
        range: String,
        accessToken: String
    ): ClearValuesResponse {
        return httpClient.post("$baseUrl/$spreadsheetId/values/$range:clear") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(mapOf<String, String>())
        }.googleSheetsBody()
    }

    suspend fun batchUpdateValues(
        spreadsheetId: String,
        request: BatchUpdateValuesRequest,
        accessToken: String
    ): BatchUpdateValuesResponse {
        return httpClient.post("$baseUrl/$spreadsheetId/values:batchUpdate") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.googleSheetsBody()
    }

    suspend fun batchClearValues(
        spreadsheetId: String,
        request: BatchClearValuesRequest,
        accessToken: String
    ): BatchClearValuesResponse {
        return httpClient.post("$baseUrl/$spreadsheetId/values:batchClear") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.googleSheetsBody()
    }

    suspend fun batchUpdate(
        spreadsheetId: String,
        request: BatchUpdateSpreadsheetRequest,
        accessToken: String
    ): BatchUpdateSpreadsheetResponse {
        return httpClient.post("$baseUrl/$spreadsheetId:batchUpdate") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.googleSheetsBody()
    }
}

private suspend inline fun <reified T> HttpResponse.googleSheetsBody(): T {
    ensureGoogleSheetsSuccess()
    return body()
}

private suspend fun HttpResponse.ensureGoogleSheetsSuccess(): HttpResponse {
    if (status.value in 200..299) return this

    val responseBody = bodyAsText().take(MAX_ERROR_BODY_LENGTH)
    error("Google Sheets API error ${status.value} ${status.description}: $responseBody")
}

private const val MAX_ERROR_BODY_LENGTH = 2_000
