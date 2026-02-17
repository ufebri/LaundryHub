package com.raylabs.laundryhub.ui.profile

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearCacheUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.GetCacheSizeUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SetShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.dummy.profile.dummyProfileUiState
import com.raylabs.laundryhub.ui.profile.state.toUI
import junit.framework.TestCase.assertEquals
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
        `when`(observeShowWhatsAppSettingUseCase.invoke()).thenReturn(flowOf(true))
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
        // Given
        val user = User("id", "Ray Febri", "uray@mail.com", "photoUrl")
        `when`(userUseCase.getCurrentUser()).thenReturn(user)

        // When
        viewModel = ProfileViewModel(
            userUseCase,
            observeShowWhatsAppSettingUseCase,
            setShowWhatsAppSettingUseCase,
            getCacheSizeUseCase,
            clearCacheUseCase
        )
        val actual = viewModel.uiState.value.user.data

        // Then
        assertEquals(user.toUI(), actual)
        assertEquals(dummyProfileUiState.user.data?.displayName, actual?.displayName)
    }

    @Test
    fun `logOut updates state and calls onSuccess`() = runTest {
        // Given
        `when`(userUseCase.signOut()).thenReturn(true)
        viewModel = ProfileViewModel(
            userUseCase,
            observeShowWhatsAppSettingUseCase,
            setShowWhatsAppSettingUseCase,
            getCacheSizeUseCase,
            clearCacheUseCase
        )
        var called = false

        // When
        viewModel.logOut { called = true }
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.logout.data == true)
        assertTrue(called)
    }

    @Test
    fun `logOut updates state with false if failed`() = runTest {
        // Given
        `when`(userUseCase.signOut()).thenReturn(false)
        viewModel = ProfileViewModel(
            userUseCase,
            observeShowWhatsAppSettingUseCase,
            setShowWhatsAppSettingUseCase,
            getCacheSizeUseCase,
            clearCacheUseCase
        )
        var called = false

        // When
        viewModel.logOut { called = true }
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(false, state.logout.data)
        assertTrue(!called)
    }

    @Test
    fun `setShowWhatsAppOption calls use case`() = runTest {
        viewModel = ProfileViewModel(
            userUseCase,
            observeShowWhatsAppSettingUseCase,
            setShowWhatsAppSettingUseCase,
            getCacheSizeUseCase,
            clearCacheUseCase
        )
        viewModel.setShowWhatsAppOption(false)
        advanceUntilIdle()

        verify(setShowWhatsAppSettingUseCase).invoke(false)
    }

    @Test
    fun `observe settings updates showWhatsAppOption`() = runTest {
        `when`(observeShowWhatsAppSettingUseCase.invoke()).thenReturn(flowOf(false))
        viewModel = ProfileViewModel(
            userUseCase,
            observeShowWhatsAppSettingUseCase,
            setShowWhatsAppSettingUseCase,
            getCacheSizeUseCase,
            clearCacheUseCase
        )
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.showWhatsAppOption)
    }

    @Test
    fun `fetch cache size updates state`() = runTest {
        `when`(getCacheSizeUseCase.invoke()).thenReturn(1024L)

        viewModel = ProfileViewModel(
            userUseCase,
            observeShowWhatsAppSettingUseCase,
            setShowWhatsAppSettingUseCase,
            getCacheSizeUseCase,
            clearCacheUseCase
        )
        advanceUntilIdle()

        assertEquals(1024L, viewModel.uiState.value.cacheSize.data)
    }

    @Test
    fun `clear cache updates state and refreshes size`() = runTest {
        `when`(clearCacheUseCase.invoke()).thenReturn(true)
        `when`(getCacheSizeUseCase.invoke()).thenReturn(2048L, 0L)

        viewModel = ProfileViewModel(
            userUseCase,
            observeShowWhatsAppSettingUseCase,
            setShowWhatsAppSettingUseCase,
            getCacheSizeUseCase,
            clearCacheUseCase
        )
        advanceUntilIdle()

        viewModel.openClearCacheDialog()
        viewModel.clearCache()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.clearCache.data)
        assertEquals(0L, viewModel.uiState.value.cacheSize.data)
        assertTrue(!viewModel.uiState.value.showClearCacheDialog)
    }
}
