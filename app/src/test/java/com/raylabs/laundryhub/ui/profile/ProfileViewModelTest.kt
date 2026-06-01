package com.raylabs.laundryhub.ui.profile

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearCacheUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.GetCacheSizeUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SetShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val userUseCase: UserUseCase = mock()
    private val observeReminderSettingsUseCase: ObserveReminderSettingsUseCase = mock()
    private val observeShowWhatsAppSettingUseCase: ObserveShowWhatsAppSettingUseCase = mock()
    private val setShowWhatsAppSettingUseCase: SetShowWhatsAppSettingUseCase = mock()
    private val getCacheSizeUseCase: GetCacheSizeUseCase = mock()
    private val clearCacheUseCase: ClearCacheUseCase = mock()

    private val mockUser = User(
        uid = "user-123",
        displayName = "John Doe",
        email = "john@example.com",
        urlPhoto = "https://example.com/photo.jpg"
    )

    private val mockReminderSettings = ReminderSettings(
        isReminderEnabled = true,
        isDailyNotificationEnabled = true,
        notificationHour = 8,
        notificationMinute = 30
    )

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        whenever(userUseCase.getCurrentUser()).thenReturn(mockUser)
        whenever(observeReminderSettingsUseCase()).thenReturn(flowOf(mockReminderSettings))
        whenever(observeShowWhatsAppSettingUseCase()).thenReturn(flowOf(true))
        whenever(getCacheSizeUseCase()).thenReturn(1024L)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init fetches user, observes settings, observes reminder settings, and fetches cache size`() = runTest {
        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("John Doe", state.user.data?.displayName)
        assertEquals("john@example.com", state.user.data?.email)
        assertEquals("https://example.com/photo.jpg", state.user.data?.photoUrl)
        assertTrue(state.showWhatsAppOption)
        assertEquals(mockReminderSettings, state.reminderSettings)
        assertEquals(1024L, state.cacheSize.data)
        assertFalse(state.cacheSize.isLoading)
    }

    @Test
    fun `fetchCacheSize sets error state when getCacheSize throws exception`() = runTest {
        whenever(getCacheSizeUseCase()).thenThrow(RuntimeException("Cache size error"))
        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Cache size error", state.cacheSize.errorMessage)
        assertFalse(state.cacheSize.isLoading)
    }

    @Test
    fun `setShowWhatsAppOption delegates setting to use case`() = runTest {
        val viewModel = createViewModel()
        viewModel.setShowWhatsAppOption(false)
        dispatcher.scheduler.advanceUntilIdle()

        verify(setShowWhatsAppSettingUseCase).invoke(false)
    }

    @Test
    fun `logOut triggers onSuccess when signout is successful`() = runTest {
        whenever(userUseCase.signOut()).thenReturn(true)
        val viewModel = createViewModel()
        var successTriggered = false

        viewModel.logOut { successTriggered = true }
        
        // Assert it starts loading
        assertTrue(viewModel.uiState.value.logout.isLoading)

        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(successTriggered)
    }

    @Test
    fun `logOut does not trigger onSuccess when signout is unsuccessful`() = runTest {
        whenever(userUseCase.signOut()).thenReturn(false)
        val viewModel = createViewModel()
        var successTriggered = false

        viewModel.logOut { successTriggered = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(successTriggered)
    }

    @Test
    fun `openClearCacheDialog sets showClearCacheDialog to true`() = runTest {
        val viewModel = createViewModel()
        viewModel.openClearCacheDialog()
        assertTrue(viewModel.uiState.value.showClearCacheDialog)
    }

    @Test
    fun `dismissClearCacheDialog sets showClearCacheDialog to false`() = runTest {
        val viewModel = createViewModel()
        viewModel.openClearCacheDialog()
        viewModel.dismissClearCacheDialog()
        assertFalse(viewModel.uiState.value.showClearCacheDialog)
    }

    @Test
    fun `clearCache successfully clears cache and updates states`() = runTest {
        whenever(clearCacheUseCase()).thenReturn(true)
        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        // Clear cache
        viewModel.clearCache()

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.clearCache.data)
        assertFalse(state.clearCache.isLoading)
        assertFalse(state.showClearCacheDialog)
        // verify fetchCacheSize is called again (once on init, once post-clear)
        verify(getCacheSizeUseCase, times(2)).invoke()
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            userUseCase = userUseCase,
            observeReminderSettingsUseCase = observeReminderSettingsUseCase,
            observeShowWhatsAppSettingUseCase = observeShowWhatsAppSettingUseCase,
            setShowWhatsAppSettingUseCase = setShowWhatsAppSettingUseCase,
            getCacheSizeUseCase = getCacheSizeUseCase,
            clearCacheUseCase = clearCacheUseCase
        )
    }
}
