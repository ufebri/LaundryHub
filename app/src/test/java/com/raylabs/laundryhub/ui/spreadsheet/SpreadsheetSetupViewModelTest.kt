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
