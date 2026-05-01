package com.raylabs.laundryhub.backend.service

import com.google.auth.oauth2.GoogleCredentials
import com.raylabs.laundryhub.shared.network.HttpClientProvider
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

class SheetsSyncService {

    private val httpClient = HttpClientProvider.createClient(enableLogging = true)
    private val sheetsApiClient = GoogleSheetsApiClient(httpClient)

    /**
     * Gets a Google API Access token using a Service Account JSON.
     * In a production environment, this uses google-auth-library-oauth2-http
     * to exchange the GOOGLE_SERVICE_ACCOUNT_JSON environment variable for a short-lived token.
     */
    private fun getServiceAccountToken(): String {
        val jsonEnv = System.getenv("GOOGLE_SERVICE_ACCOUNT_JSON")
        if (jsonEnv.isNullOrBlank()) {
            throw IllegalStateException("GOOGLE_SERVICE_ACCOUNT_JSON environment variable is not set")
        }
        
        val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(jsonEnv.toByteArray()))
            .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
        
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    /**
     * Sync data to a Google Sheet automatically using a Service Account.
     */
    suspend fun syncDataToSheet(spreadsheetId: String, range: String, accessToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = accessToken ?: getServiceAccountToken()

            // Simulated data to append/update
            val mockValues = listOf(
                listOf("Backend-Sync-Auto", "System User", "0", "2024-06-01")
            )

            val valueRange = ValueRange(
                range = range,
                majorDimension = "ROWS",
                values = mockValues
            )

            val response = sheetsApiClient.appendValues(
                spreadsheetId = spreadsheetId,
                range = range,
                valueRange = valueRange,
                accessToken = token
            )

            response.updates != null
        } catch (e: Exception) {
            println("Error syncing to Google Sheets: ${e.message}")
            false
        }
    }
}

