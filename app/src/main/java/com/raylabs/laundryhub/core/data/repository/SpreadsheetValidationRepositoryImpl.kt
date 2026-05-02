package com.raylabs.laundryhub.core.data.repository

import android.util.Log
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.data.service.SpreadsheetIdParser
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.core.domain.model.sheets.*
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetValidationRepository
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.serialization.json.*

class SpreadsheetValidationRepositoryImpl @Inject constructor(
    private val apiClient: GoogleSheetsApiClient,
    private val authManager: GoogleSheetsAuthorizationManager
) : SpreadsheetValidationRepository {

    override suspend fun validateSpreadsheet(input: String): Resource<SpreadsheetValidationResult> {
        return withContext(Dispatchers.IO) {
            val spreadsheetId = SpreadsheetIdParser.normalize(input)
                ?: return@withContext Resource.Error("Invalid spreadsheet URL or ID.")

            try {
                val token = authManager.getAccessToken() ?: return@withContext Resource.Error("Token expired")
                
                // Validate existence and title via metadata
                val metadataJson = apiClient.getSpreadsheet(spreadsheetId, token)
                val metadata = Json.parseToJsonElement(metadataJson).jsonObject
                
                val spreadsheetTitle = metadata["properties"]?.jsonObject?.get("title")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                    ?: "LaundryHub Spreadsheet"

                val sheetNames = metadata["sheets"]?.jsonArray?.mapNotNull { 
                    it.jsonObject["properties"]?.jsonObject?.get("title")?.jsonPrimitive?.content?.trim()?.lowercase() 
                }?.toSet().orEmpty()

                val missingSheets = REQUIRED_SHEETS - sheetNames
                if (missingSheets.isNotEmpty()) {
                    return@withContext Resource.Error(
                        "Spreadsheet template is incomplete. Missing sheets: ${missingSheets.sorted().joinToString(", ")}."
                    )
                }

                // Header validations
                validateHeaders(spreadsheetId, "gross!A1:D1", "gross", setOf(GROSS_MONTH, GROSS_TOTAL_NOMINAL, GROSS_ORDER_COUNT, GROSS_TAX), token)?.let { return@withContext it }
                validateHeaders(spreadsheetId, "income!A1:L1", "income", setOf("orderID", "Date", "Name", "Weight", "Price/kg", "Total Price", "(lunas/belum)", "Package", "remark", "payment", "phoneNumber", "due date"), token)?.let { return@withContext it }
                validateHeaders(spreadsheetId, "notes!A1:D1", "notes", setOf("harga", "packages", "work", "unit"), token)?.let { return@withContext it }
                validateHeaders(spreadsheetId, "outcome!A1:F1", "outcome", setOf("id", "date", "keperluan", "price", "remark", "payment"), token)?.let { return@withContext it }

                Resource.Success(
                    SpreadsheetValidationResult(
                        spreadsheetId = spreadsheetId,
                        spreadsheetTitle = spreadsheetTitle
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Spreadsheet validation failed: ${e.message}", e)
                Resource.Error(e.message ?: "Unknown error during validation")
            }
        }
    }

    private suspend fun validateHeaders(
        spreadsheetId: String,
        range: String,
        sheetName: String,
        requiredHeaders: Set<String>,
        token: String
    ): Resource.Error? {
        val response = apiClient.getValues(spreadsheetId, range, token)
        val headerValues = response.values?.firstOrNull()?.map { it.trim().lowercase() }?.toSet().orEmpty()

        val normalizedRequiredHeaders = requiredHeaders.map { it.lowercase() }.toSet()
        val missingHeaders = normalizedRequiredHeaders - headerValues

        return if (missingHeaders.isNotEmpty()) {
            Resource.Error("Sheet \"$sheetName\" is missing required columns: ${missingHeaders.sorted().joinToString(", ")}.")
        } else {
            null
        }
    }

    private companion object {
        const val TAG = "SpreadsheetValidation"
        val REQUIRED_SHEETS = setOf("summary", "gross", "income", "notes", "outcome")
    }
}
