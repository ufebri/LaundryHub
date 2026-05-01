package com.raylabs.laundryhub.core.data.repository

import android.util.Log
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.data.service.SpreadsheetIdParser
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.core.domain.model.sheets.GROSS_MONTH
import com.raylabs.laundryhub.core.domain.model.sheets.GROSS_ORDER_COUNT
import com.raylabs.laundryhub.core.domain.model.sheets.GROSS_TAX
import com.raylabs.laundryhub.core.domain.model.sheets.GROSS_TOTAL_NOMINAL
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetValidationRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SpreadsheetValidationRepositoryImpl @Inject constructor(
    private val googleSheetService: GoogleSheetService
) : SpreadsheetValidationRepository {

    override suspend fun validateSpreadsheet(input: String): Resource<SpreadsheetValidationResult> {
        return withContext(Dispatchers.IO) {
            val spreadsheetId = SpreadsheetIdParser.normalize(input)
                ?: return@withContext Resource.Error("Invalid spreadsheet URL or ID.")

            try {
                Log.d(TAG, "Validating spreadsheet input=$input normalizedId=$spreadsheetId")
                val spreadsheet = googleSheetService.getSheetsService().spreadsheets()
                    .get(spreadsheetId)
                    .execute()
                val spreadsheetTitle =
                    spreadsheet.properties?.title?.takeIf { it.isNotBlank() }
                        ?: "LaundryHub Spreadsheet"

                val sheetNames = spreadsheet.sheets
                    ?.mapNotNull { it.properties?.title?.trim()?.lowercase() }
                    ?.toSet()
                    .orEmpty()

                val missingSheets = REQUIRED_SHEETS - sheetNames
                if (missingSheets.isNotEmpty()) {
                    return@withContext Resource.Error(
                        "Spreadsheet template is incomplete. Missing sheets: ${
                            missingSheets.sorted().joinToString(", ")
                        }."
                    )
                }

                validateHeaders(
                    spreadsheetId = spreadsheetId,
                    range = "gross!A1:D1",
                    sheetName = "gross",
                    requiredHeaders = setOf(
                        GROSS_MONTH,
                        GROSS_TOTAL_NOMINAL,
                        GROSS_ORDER_COUNT,
                        GROSS_TAX
                    )
                )?.let { return@withContext it }

                validateHeaders(
                    spreadsheetId = spreadsheetId,
                    range = "income!A1:L1",
                    sheetName = "income",
                    requiredHeaders = setOf(
                        "orderID",
                        "Date",
                        "Name",
                        "Weight",
                        "Price/kg",
                        "Total Price",
                        "(lunas/belum)",
                        "Package",
                        "remark",
                        "payment",
                        "phoneNumber",
                        "due date"
                    )
                )?.let { return@withContext it }

                validateHeaders(
                    spreadsheetId = spreadsheetId,
                    range = "notes!A1:D1",
                    sheetName = "notes",
                    requiredHeaders = setOf("harga", "packages", "work", "unit")
                )?.let { return@withContext it }

                validateHeaders(
                    spreadsheetId = spreadsheetId,
                    range = "outcome!A1:F1",
                    sheetName = "outcome",
                    requiredHeaders = setOf("id", "date", "keperluan", "price", "remark", "payment")
                )?.let { return@withContext it }

                validateWriteAccess(
                    spreadsheetId = spreadsheetId
                )?.let { return@withContext it }

                Resource.Success(
                    SpreadsheetValidationResult(
                        spreadsheetId = spreadsheetId,
                        spreadsheetTitle = spreadsheetTitle
                    )
                )
            } catch (e: GoogleJsonResponseException) {
                Log.e(TAG, "Spreadsheet validation failed with GoogleJsonResponseException status=${e.statusCode} message=${e.details?.message}", e)
                GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(e)
            } catch (e: Exception) {
                Log.e(TAG, "Spreadsheet validation failed: ${e.message}", e)
                GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
            }
        }
    }

    private fun validateHeaders(
        spreadsheetId: String,
        range: String,
        sheetName: String,
        requiredHeaders: Set<String>
    ): Resource.Error? {
        val headerValues = googleSheetService.getSheetsService().spreadsheets().values()
            .get(spreadsheetId, range)
            .execute()
            .getValues()
            ?.firstOrNull()
            ?.map { it.toString().trim().lowercase() }
            ?.toSet()
            .orEmpty()

        val normalizedRequiredHeaders = requiredHeaders.map { it.lowercase() }.toSet()
        val missingHeaders = normalizedRequiredHeaders - headerValues

        return if (missingHeaders.isNotEmpty()) {
            Resource.Error(
                "Sheet \"$sheetName\" is missing required columns: ${
                    missingHeaders.sorted().joinToString(", ")
                }."
            )
        } else {
            null
        }
    }

    private fun validateWriteAccess(
        spreadsheetId: String
    ): Resource.Error? {
        return if (googleSheetService.hasSpreadsheetEditAccess(spreadsheetId)) {
            null
        } else {
            Resource.Error(GSheetRepositoryErrorHandling.EDIT_ACCESS_REQUIRED_MESSAGE)
        }
    }

    private companion object {
        const val TAG = "SpreadsheetValidation"
        val REQUIRED_SHEETS = setOf("summary", "gross", "income", "notes", "outcome")
    }
}
