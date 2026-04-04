package com.raylabs.laundryhub.ui.profile

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearCacheUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearSpreadsheetConnectionUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.GetCacheSizeUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveSpreadsheetConfigUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SaveSpreadsheetConnectionUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SetShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ValidateSpreadsheetUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.dummy.profile.dummyProfileUiState
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.profile.state.toUI
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var userUseCase: UserUseCase
    private lateinit var observeShowWhatsAppSettingUseCase: ObserveShowWhatsAppSettingUseCase
    private lateinit var setShowWhatsAppSettingUseCase: SetShowWhatsAppSettingUseCase
    private lateinit var getCacheSizeUseCase: GetCacheSizeUseCase
    private lateinit var clearCacheUseCase: ClearCacheUseCase
    private lateinit var observeSpreadsheetConfigUseCase: ObserveSpreadsheetConfigUseCase
    private lateinit var clearSpreadsheetConnectionUseCase: ClearSpreadsheetConnectionUseCase
    private lateinit var saveSpreadsheetConnectionUseCase: SaveSpreadsheetConnectionUseCase
    private lateinit var validateSpreadsheetUseCase: ValidateSpreadsheetUseCase
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userUseCase = mock(UserUseCase::class.java)
        observeShowWhatsAppSettingUseCase = mock(ObserveShowWhatsAppSettingUseCase::class.java)
        setShowWhatsAppSettingUseCase = mock(SetShowWhatsAppSettingUseCase::class.java)
        getCacheSizeUseCase = mock(GetCacheSizeUseCase::class.java)
        clearCacheUseCase = mock(ClearCacheUseCase::class.java)
        observeSpreadsheetConfigUseCase = mock(ObserveSpreadsheetConfigUseCase::class.java)
        clearSpreadsheetConnectionUseCase = mock(ClearSpreadsheetConnectionUseCase::class.java)
        saveSpreadsheetConnectionUseCase = mock(SaveSpreadsheetConnectionUseCase::class.java)
        validateSpreadsheetUseCase = mock(ValidateSpreadsheetUseCase::class.java)

        `when`(observeShowWhatsAppSettingUseCase.invoke()).thenReturn(flowOf(true))
        `when`(observeSpreadsheetConfigUseCase.invoke()).thenReturn(flowOf(SpreadsheetConfig()))
        runBlocking {
            `when`(getCacheSizeUseCase.invoke()).thenReturn(0L)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchUser sets user in uiState`() = runTest {
        val user = User("id", "Ray Febri", "uray@mail.com", "photoUrl")
        `when`(userUseCase.getCurrentUser()).thenReturn(user)

        viewModel = createViewModel()
        val actual = viewModel.uiState.value.user.data

        assertEquals(user.toUI(), actual)
        assertEquals(dummyProfileUiState.user.data?.displayName, actual?.displayName)
    }

    @Test
    fun `logOut updates state and calls onSuccess`() = runTest {
        `when`(userUseCase.signOut()).thenReturn(true)
        viewModel = createViewModel()
        var called = false

        viewModel.logOut { called = true }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.logout.data == true)
        assertTrue(called)
    }

    @Test
    fun `logOut updates state with false if failed`() = runTest {
        `when`(userUseCase.signOut()).thenReturn(false)
        viewModel = createViewModel()
        var called = false

        viewModel.logOut { called = true }
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.logout.data)
        assertTrue(!called)
    }

    @Test
    fun `setShowWhatsAppOption calls use case`() = runTest {
        viewModel = createViewModel()

        viewModel.setShowWhatsAppOption(false)
        advanceUntilIdle()

        verify(setShowWhatsAppSettingUseCase).invoke(false)
    }

    @Test
    fun `observe settings updates showWhatsAppOption`() = runTest {
        `when`(observeShowWhatsAppSettingUseCase.invoke()).thenReturn(flowOf(false))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.showWhatsAppOption)
    }

    @Test
    fun `fetch cache size updates state`() = runTest {
        `when`(getCacheSizeUseCase.invoke()).thenReturn(1024L)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1024L, viewModel.uiState.value.cacheSize.data)
    }

    @Test
    fun `fetch cache size stores error when use case fails`() = runTest {
        runBlocking {
            `when`(getCacheSizeUseCase.invoke()).thenThrow(RuntimeException("cache unavailable"))
        }

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("cache unavailable", viewModel.uiState.value.cacheSize.errorMessage)
    }

    @Test
    fun `open and dismiss clear cache dialog update state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openClearCacheDialog()
        assertTrue(viewModel.uiState.value.showClearCacheDialog)

        viewModel.dismissClearCacheDialog()
        assertFalse(viewModel.uiState.value.showClearCacheDialog)
    }

    @Test
    fun `clear cache updates state and refreshes size`() = runTest {
        `when`(clearCacheUseCase.invoke()).thenReturn(true)
        `when`(getCacheSizeUseCase.invoke()).thenReturn(2048L, 0L)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openClearCacheDialog()
        viewModel.clearCache()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.clearCache.data)
        assertEquals(0L, viewModel.uiState.value.cacheSize.data)
        assertTrue(!viewModel.uiState.value.showClearCacheDialog)
    }

    @Test
    fun `clear cache stores error and still refreshes cache size`() = runTest {
        runBlocking {
            `when`(clearCacheUseCase.invoke()).thenThrow(RuntimeException("clear cache failed"))
            `when`(getCacheSizeUseCase.invoke()).thenReturn(2048L, 1024L)
        }

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openClearCacheDialog()
        viewModel.clearCache()
        advanceUntilIdle()

        assertEquals("clear cache failed", viewModel.uiState.value.clearCache.errorMessage)
        assertEquals(1024L, viewModel.uiState.value.cacheSize.data)
        assertFalse(viewModel.uiState.value.showClearCacheDialog)
    }

    @Test
    fun `observe spreadsheet config updates connected spreadsheet state`() = runTest {
        `when`(observeSpreadsheetConfigUseCase.invoke()).thenReturn(
            flowOf(
                SpreadsheetConfig(
                    spreadsheetId = "sheet-123",
                    spreadsheetName = "Laundry A",
                    spreadsheetUrl = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
                )
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Laundry A", viewModel.uiState.value.connectedSpreadsheet.data?.spreadsheetName)
        assertEquals("sheet-123", viewModel.uiState.value.connectedSpreadsheet.data?.spreadsheetId)
    }

    @Test
    fun `revalidate spreadsheet stores success message and refreshes saved spreadsheet`() = runTest {
        `when`(observeSpreadsheetConfigUseCase.invoke()).thenReturn(
            flowOf(
                SpreadsheetConfig(
                    spreadsheetId = "sheet-123",
                    spreadsheetName = "Laundry A",
                    spreadsheetUrl = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
                )
            )
        )
        runBlocking {
            `when`(
                validateSpreadsheetUseCase.invoke("https://docs.google.com/spreadsheets/d/sheet-123/edit")
            ).thenReturn(
                Resource.Success(
                    SpreadsheetValidationResult(
                        spreadsheetId = "sheet-123",
                        spreadsheetTitle = "Laundry A Updated"
                    )
                )
            )
        }

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.revalidateSpreadsheet()
        advanceUntilIdle()

        assertEquals(
            "Spreadsheet validated successfully.",
            viewModel.uiState.value.spreadsheetValidation.data
        )
        runBlocking {
            verify(saveSpreadsheetConnectionUseCase).invoke(
                "sheet-123",
                "Laundry A Updated",
                "https://docs.google.com/spreadsheets/d/sheet-123/edit"
            )
        }
    }

    @Test
    fun `revalidate spreadsheet stores error message when validation fails`() = runTest {
        `when`(observeSpreadsheetConfigUseCase.invoke()).thenReturn(
            flowOf(
                SpreadsheetConfig(
                    spreadsheetId = "sheet-123",
                    spreadsheetName = "Laundry A",
                    spreadsheetUrl = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
                )
            )
        )
        runBlocking {
            `when`(
                validateSpreadsheetUseCase.invoke("https://docs.google.com/spreadsheets/d/sheet-123/edit")
            ).thenReturn(Resource.Error("Error 403: access denied"))
        }

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.revalidateSpreadsheet()
        advanceUntilIdle()

        assertEquals(
            "Error 403: access denied",
            viewModel.uiState.value.spreadsheetValidation.errorMessage
        )
    }

    @Test
    fun `open and dismiss change spreadsheet dialog update state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openChangeSpreadsheetDialog()
        assertTrue(viewModel.uiState.value.showChangeSpreadsheetDialog)

        viewModel.dismissChangeSpreadsheetDialog()
        assertFalse(viewModel.uiState.value.showChangeSpreadsheetDialog)
    }

    @Test
    fun `confirm change spreadsheet clears connection and closes dialog`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openChangeSpreadsheetDialog()
        viewModel.confirmChangeSpreadsheet()
        advanceUntilIdle()

        verify(clearSpreadsheetConnectionUseCase).invoke()
        assertFalse(viewModel.uiState.value.showChangeSpreadsheetDialog)
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            userUseCase = userUseCase,
            observeShowWhatsAppSettingUseCase = observeShowWhatsAppSettingUseCase,
            setShowWhatsAppSettingUseCase = setShowWhatsAppSettingUseCase,
            getCacheSizeUseCase = getCacheSizeUseCase,
            clearCacheUseCase = clearCacheUseCase,
            observeSpreadsheetConfigUseCase = observeSpreadsheetConfigUseCase,
            clearSpreadsheetConnectionUseCase = clearSpreadsheetConnectionUseCase,
            saveSpreadsheetConnectionUseCase = saveSpreadsheetConnectionUseCase,
            validateSpreadsheetUseCase = validateSpreadsheetUseCase
        )
    }
}
