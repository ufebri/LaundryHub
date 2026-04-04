package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpResponseException
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SpreadsheetValidationRepositoryImplTest {

    @Test
    fun `validateSpreadsheet returns invalid input error when spreadsheet id cannot be parsed`() = runTest {
        val repository = createRepository()

        val result = repository.validateSpreadsheet("not-a-sheet")

        assertEquals(
            Resource.Error("Invalid spreadsheet URL or ID."),
            result
        )
    }

    @Test
    fun `validateSpreadsheet succeeds when spreadsheet is readable and editable`() = runTest {
        val repository = createRepository()

        val result = repository.validateSpreadsheet(INPUT_URL)

        assertEquals(
            Resource.Success(
                SpreadsheetValidationResult(
                    spreadsheetId = SHEET_ID,
                    spreadsheetTitle = "Laundry A"
                )
            ),
            result
        )
    }

    @Test
    fun `validateSpreadsheet returns editor access error when spreadsheet is view only`() = runTest {
        val repository = createRepository(
            hasEditAccess = false
        )

        val result = repository.validateSpreadsheet(INPUT_URL)

        assertEquals(
            Resource.Error(GSheetRepositoryErrorHandling.EDIT_ACCESS_REQUIRED_MESSAGE),
            result
        )
    }

    @Test
    fun `validateSpreadsheet returns missing sheets when template is incomplete`() = runTest {
        val incompleteSpreadsheet = Spreadsheet()
            .setProperties(SpreadsheetProperties().setTitle("Laundry A"))
            .setSheets(
                listOf(
                    sheet("summary"),
                    sheet("gross"),
                    sheet("income")
                )
            )
        val repository = createRepository(spreadsheet = incompleteSpreadsheet)

        val result = repository.validateSpreadsheet(INPUT_URL)

        assertEquals(
            Resource.Error("Spreadsheet template is incomplete. Missing sheets: notes, outcome."),
            result
        )
    }

    @Test
    fun `validateSpreadsheet returns missing headers when required column is absent`() = runTest {
        val googleSheetService: GoogleSheetService = mock()
        val sheets: Sheets = mock()
        val spreadsheetsApi: Sheets.Spreadsheets = mock()
        val spreadsheetGet: Sheets.Spreadsheets.Get = mock()
        val valuesApi: Sheets.Spreadsheets.Values = mock()
        val grossHeaderGet: Sheets.Spreadsheets.Values.Get = mock()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheetsApi)
        whenever(spreadsheetsApi.get(SHEET_ID)).thenReturn(spreadsheetGet)
        whenever(spreadsheetGet.execute()).thenReturn(validSpreadsheet())
        whenever(spreadsheetsApi.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(SHEET_ID, "gross!A1:D1")).thenReturn(grossHeaderGet)
        whenever(grossHeaderGet.execute()).thenReturn(
            headerRow("bulan", "total nominal", "# nota laundry")
        )

        val repository = SpreadsheetValidationRepositoryImpl(googleSheetService)

        val result = repository.validateSpreadsheet(INPUT_URL)

        assertEquals(
            Resource.Error("Sheet \"gross\" is missing required columns: pajak."),
            result
        )
    }

    @Test
    fun `validateSpreadsheet maps google response failures through repository handler`() = runTest {
        val googleSheetService: GoogleSheetService = mock()
        val sheets: Sheets = mock()
        val spreadsheetsApi: Sheets.Spreadsheets = mock()
        val spreadsheetGet: Sheets.Spreadsheets.Get = mock()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheetsApi)
        whenever(spreadsheetsApi.get(SHEET_ID)).thenReturn(spreadsheetGet)
        whenever(spreadsheetGet.execute()).thenThrow(
            googleJsonException(
                statusCode = 403,
                statusMessage = "Forbidden",
                detailsMessage = "Google Drive API has not been used in project 655099386324 before or it is disabled."
            )
        )

        val repository = SpreadsheetValidationRepositoryImpl(googleSheetService)

        val result = repository.validateSpreadsheet(INPUT_URL)

        assertEquals(
            Resource.Error(GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE),
            result
        )
    }

    @Test
    fun `validateSpreadsheet maps unexpected exceptions through read handler`() = runTest {
        val googleSheetService: GoogleSheetService = mock()
        whenever(googleSheetService.getSheetsService()).thenThrow(
            IllegalStateException("access token is unavailable for owner@laundryhub.com")
        )

        val repository = SpreadsheetValidationRepositoryImpl(googleSheetService)

        val result = repository.validateSpreadsheet(INPUT_URL)

        assertEquals(
            Resource.Error(GSheetRepositoryErrorHandling.AUTHORIZATION_CONFIGURATION_MESSAGE),
            result
        )
    }

    private fun createRepository(
        spreadsheet: Spreadsheet = validSpreadsheet(),
        hasEditAccess: Boolean = true
    ): SpreadsheetValidationRepositoryImpl {
        val googleSheetService: GoogleSheetService = mock()
        val sheets: Sheets = mock()
        val spreadsheetsApi: Sheets.Spreadsheets = mock()
        val spreadsheetGet: Sheets.Spreadsheets.Get = mock()
        val valuesApi: Sheets.Spreadsheets.Values = mock()
        val grossHeaderGet: Sheets.Spreadsheets.Values.Get = mock()
        val incomeHeaderGet: Sheets.Spreadsheets.Values.Get = mock()
        val notesHeaderGet: Sheets.Spreadsheets.Values.Get = mock()
        val outcomeHeaderGet: Sheets.Spreadsheets.Values.Get = mock()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheetsApi)
        whenever(spreadsheetsApi.get(SHEET_ID)).thenReturn(spreadsheetGet)
        whenever(spreadsheetGet.execute()).thenReturn(spreadsheet)
        whenever(spreadsheetsApi.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(SHEET_ID, "gross!A1:D1")).thenReturn(grossHeaderGet)
        whenever(valuesApi.get(SHEET_ID, "income!A1:L1")).thenReturn(incomeHeaderGet)
        whenever(valuesApi.get(SHEET_ID, "notes!A1:D1")).thenReturn(notesHeaderGet)
        whenever(valuesApi.get(SHEET_ID, "outcome!A1:F1")).thenReturn(outcomeHeaderGet)
        whenever(grossHeaderGet.execute()).thenReturn(
            headerRow("bulan", "total nominal", "# nota laundry", "pajak")
        )
        whenever(incomeHeaderGet.execute()).thenReturn(
            headerRow(
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
        )
        whenever(notesHeaderGet.execute()).thenReturn(
            headerRow("harga", "packages", "work", "unit")
        )
        whenever(outcomeHeaderGet.execute()).thenReturn(
            headerRow("id", "date", "keperluan", "price", "remark", "payment")
        )
        whenever(googleSheetService.hasSpreadsheetEditAccess(SHEET_ID)).thenReturn(hasEditAccess)

        return SpreadsheetValidationRepositoryImpl(googleSheetService)
    }

    private fun validSpreadsheet(): Spreadsheet = Spreadsheet()
        .setProperties(SpreadsheetProperties().setTitle("Laundry A"))
        .setSheets(
            listOf(
                sheet("summary"),
                sheet("gross"),
                sheet("income"),
                sheet("notes"),
                sheet("outcome")
            )
        )

    private fun sheet(name: String): Sheet =
        Sheet().setProperties(SheetProperties().setTitle(name))

    private fun headerRow(vararg values: String): ValueRange =
        ValueRange().setValues(listOf(values.toList()))

    private fun googleJsonException(
        statusCode: Int,
        statusMessage: String,
        detailsMessage: String
    ): GoogleJsonResponseException {
        val error = GoogleJsonError().apply {
            message = detailsMessage
        }
        val builder = HttpResponseException.Builder(
            statusCode,
            statusMessage,
            HttpHeaders()
        ).setContent(detailsMessage)
        return GoogleJsonResponseException(builder, error)
    }

    private companion object {
        private const val SHEET_ID = "sheet-123"
        private const val INPUT_URL = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
    }
}
