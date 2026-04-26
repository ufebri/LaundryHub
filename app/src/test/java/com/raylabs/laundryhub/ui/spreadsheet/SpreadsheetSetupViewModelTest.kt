package com.raylabs.laundryhub.ui.spreadsheet

import com.raylabs.laundryhub.core.data.repository.GSheetRepositoryErrorHandling
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveSpreadsheetConfigUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SaveSpreadsheetConnectionUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ValidateSpreadsheetUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SpreadsheetSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val spreadsheetConfigFlow = MutableStateFlow(SpreadsheetConfig())
    private lateinit var observeSpreadsheetConfigUseCase: ObserveSpreadsheetConfigUseCase
    private lateinit var saveSpreadsheetConnectionUseCase: SaveSpreadsheetConnectionUseCase
    private lateinit var validateSpreadsheetUseCase: ValidateSpreadsheetUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        observeSpreadsheetConfigUseCase = mock()
        saveSpreadsheetConnectionUseCase = mock()
        validateSpreadsheetUseCase = mock()
        spreadsheetConfigFlow.value = SpreadsheetConfig()
        whenever(observeSpreadsheetConfigUseCase.invoke()).thenReturn(spreadsheetConfigFlow)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `validateAndContinue saves connection and marks state ready on success`() = runTest {
        whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(
            Resource.Success(
                SpreadsheetValidationResult(
                    spreadsheetId = SHEET_ID,
                    spreadsheetTitle = "Laundry A"
                )
            )
        )

        val viewModel = createViewModel()
        viewModel.onInputChanged(INPUT_URL)
        viewModel.validateAndContinue()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isReady)
        assertEquals(SHEET_ID, state.configuredSpreadsheetId)
        assertEquals("Laundry A", state.configuredSpreadsheetName)
        assertFalse(state.showRequestAccess)
        runBlocking {
            verify(saveSpreadsheetConnectionUseCase).invoke(
                spreadsheetId = SHEET_ID,
                spreadsheetName = "Laundry A",
                spreadsheetUrl = INPUT_URL
            )
        }
    }

    @Test
    fun `validateAndContinue shows required message when input is blank`() = runTest {
        val viewModel = createViewModel()
        viewModel.onInputChanged("   ")

        viewModel.validateAndContinue()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isReady)
        assertFalse(state.isValidating)
        assertFalse(state.showRequestAccess)
        assertEquals("Paste your spreadsheet URL or ID first.", state.errorMessage)
        verifyNoInteractions(validateSpreadsheetUseCase)
    }

    @Test
    fun `validateAndContinue exposes request access when spreadsheet access is denied`() = runTest {
        whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(
            Resource.Error("Error 403: access denied")
        )

        val viewModel = createViewModel()
        viewModel.onInputChanged(INPUT_URL)
        viewModel.validateAndContinue()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isReady)
        assertTrue(state.showRequestAccess)
        assertEquals(
            "This account doesn't have enough access to use this spreadsheet yet.",
            state.errorMessage
        )
    }

    @Test
    fun `validateAndContinue exposes request access when spreadsheet is readable but not editable`() =
        runTest {
            whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(
                Resource.Error(GSheetRepositoryErrorHandling.EDIT_ACCESS_REQUIRED_MESSAGE)
            )

            val viewModel = createViewModel()
            viewModel.onInputChanged(INPUT_URL)
            viewModel.validateAndContinue()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isReady)
            assertTrue(state.showRequestAccess)
            assertEquals(
                "This account can open the spreadsheet, but it still needs Editor access.",
                state.errorMessage
            )
        }

    @Test
    fun `validateAndContinue uses fallback message when validation returns non terminal state`() =
        runTest {
            whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(Resource.Empty)

            val viewModel = createViewModel()
            viewModel.onInputChanged(INPUT_URL)
            viewModel.validateAndContinue()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isReady)
            assertFalse(state.showRequestAccess)
            assertEquals(
                "We couldn't validate this spreadsheet. Try again.",
                state.errorMessage
            )
        }

    @Test
    fun `validateAndContinue keeps request access hidden when google sheets oauth is missing`() =
        runTest {
            whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(
                Resource.Error(GoogleSheetService.MISSING_ACCESS_MESSAGE)
            )

            val viewModel = createViewModel()
            viewModel.onInputChanged(INPUT_URL)
            viewModel.validateAndContinue()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showRequestAccess)
            assertEquals("Connect Google Sheets first to continue.", state.errorMessage)
        }

    @Test
    fun `validateAndContinue keeps request access hidden when drive api is not enabled`() =
        runTest {
            whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(
                Resource.Error(GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE)
            )

            val viewModel = createViewModel()
            viewModel.onInputChanged(INPUT_URL)
            viewModel.validateAndContinue()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showRequestAccess)
            assertEquals(
                "LaundryHub still needs Google Drive API enabled before it can verify spreadsheet access.",
                state.errorMessage
            )
        }

    @Test
    fun `validateAndContinue keeps request access hidden for invalid spreadsheet inputs`() =
        runTest {
            whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(
                Resource.Error("Invalid spreadsheet URL or ID")
            )

            val viewModel = createViewModel()
            viewModel.onInputChanged(INPUT_URL)
            viewModel.validateAndContinue()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showRequestAccess)
            assertEquals(
                "Use a valid spreadsheet URL or spreadsheet ID.",
                state.errorMessage
            )
        }

    @Test
    fun `validateAndContinue keeps request access hidden for authorization configuration errors`() =
        runTest {
            whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(
                Resource.Error(GSheetRepositoryErrorHandling.AUTHORIZATION_CONFIGURATION_MESSAGE)
            )

            val viewModel = createViewModel()
            viewModel.onInputChanged(INPUT_URL)
            viewModel.validateAndContinue()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showRequestAccess)
            assertEquals(
                "Google Sheets couldn't reconnect cleanly on this device. Try granting access again.",
                state.errorMessage
            )
        }

    @Test
    fun `validateAndContinue asks to reconnect when google sheets access expired`() =
        runTest {
            whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(
                Resource.Error(GSheetRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE)
            )

            val viewModel = createViewModel()
            viewModel.onInputChanged(INPUT_URL)
            viewModel.validateAndContinue()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.showRequestAccess)
            assertEquals(
                "Google Sheets access expired. Grant access again to continue.",
                state.errorMessage
            )
        }

    @Test
    fun `validateAndContinue hides request access and maps error 404 to not found message`() =
        runTest {
            whenever(validateSpreadsheetUseCase.invoke(INPUT_URL)).thenReturn(
                Resource.Error("Error 404: Spreadsheet not found")
            )

            val viewModel = createViewModel()
            viewModel.onInputChanged(INPUT_URL)
            viewModel.validateAndContinue()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showRequestAccess)
            assertEquals(
                "Spreadsheet not found. Check the URL or ID and try again.",
                state.errorMessage
            )
        }

    @Test
    fun `init marks state ready when spreadsheet is already configured`() = runTest {
        spreadsheetConfigFlow.value = SpreadsheetConfig(
            spreadsheetId = SHEET_ID,
            spreadsheetName = "Laundry A",
            spreadsheetUrl = INPUT_URL,
            validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isReady)
        assertTrue(state.hasLoadedConfiguration)
        assertEquals(SHEET_ID, state.configuredSpreadsheetId)
        assertEquals("Laundry A", state.configuredSpreadsheetName)
        assertFalse(state.isRestoring)
        verifyNoInteractions(validateSpreadsheetUseCase)
    }

    @Test
    fun `init keeps setup active when saved spreadsheet needs upgraded validation`() = runTest {
        spreadsheetConfigFlow.value = SpreadsheetConfig(
            spreadsheetId = SHEET_ID,
            spreadsheetName = "Laundry A",
            spreadsheetUrl = INPUT_URL,
            validationVersion = 0
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isReady)
        assertEquals(0, state.configuredValidationVersion)
        assertEquals(INPUT_URL, state.input)
    }

    @Test
    fun `observeSpreadsheetConfig keeps typed input when configuration identity is unchanged`() =
        runTest {
            spreadsheetConfigFlow.value = SpreadsheetConfig(
                spreadsheetId = SHEET_ID,
                spreadsheetName = "Laundry A",
                spreadsheetUrl = INPUT_URL,
                validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onInputChanged("custom-input")
            spreadsheetConfigFlow.value = spreadsheetConfigFlow.value.copy(
                spreadsheetName = "Laundry A Updated"
            )
            advanceUntilIdle()

            assertEquals("custom-input", viewModel.uiState.value.input)
            assertEquals("Laundry A Updated", viewModel.uiState.value.configuredSpreadsheetName)
        }

    @Test
    fun `observeSpreadsheetConfig replaces input when spreadsheet identity changes`() = runTest {
        spreadsheetConfigFlow.value = SpreadsheetConfig(
            spreadsheetId = SHEET_ID,
            spreadsheetName = "Laundry A",
            spreadsheetUrl = INPUT_URL,
            validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onInputChanged("custom-input")

        spreadsheetConfigFlow.value = SpreadsheetConfig(
            spreadsheetId = "sheet-b",
            spreadsheetName = "Laundry B",
            spreadsheetUrl = "https://sheet-b",
            validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("https://sheet-b", state.input)
        assertEquals("sheet-b", state.configuredSpreadsheetId)
    }

    private fun createViewModel(): SpreadsheetSetupViewModel {
        return SpreadsheetSetupViewModel(
            observeSpreadsheetConfigUseCase = observeSpreadsheetConfigUseCase,
            saveSpreadsheetConnectionUseCase = saveSpreadsheetConnectionUseCase,
            validateSpreadsheetUseCase = validateSpreadsheetUseCase
        )
    }

    private companion object {
        const val SHEET_ID = "1AbCdEfGhIjKlMnOpQrStUvWxYz1234567890"
        const val INPUT_URL =
            "https://docs.google.com/spreadsheets/d/1AbCdEfGhIjKlMnOpQrStUvWxYz1234567890/edit#gid=0"
    }
}
