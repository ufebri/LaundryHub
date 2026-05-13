package com.raylabs.laundryhub.ui.startup

import com.raylabs.laundryhub.core.domain.config.BackendConfig
import com.raylabs.laundryhub.core.domain.config.BackendConfigProvider
import com.raylabs.laundryhub.core.domain.config.BackendHealthChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartupConnectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init goes ready and activates first healthy base url`() = runTest {
        val provider = FakeBackendConfigProvider(
            config = BackendConfig(PRIMARY_BASE_URL),
            candidates = listOf(PRIMARY_BASE_URL, FALLBACK_BASE_URL)
        )
        val healthChecker = FakeBackendHealthChecker(healthyBaseUrls = setOf(PRIMARY_BASE_URL))

        val viewModel = StartupConnectionViewModel(provider, healthChecker)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartupConnectionUiState.Ready, viewModel.uiState.value)
        assertEquals(PRIMARY_BASE_URL, provider.activatedBaseUrl)
        assertEquals(listOf(PRIMARY_BASE_URL), healthChecker.checkedBaseUrls)
    }

    @Test
    fun `init shows unavailable only after every candidate fails health check`() = runTest {
        val provider = FakeBackendConfigProvider(
            config = BackendConfig(PRIMARY_BASE_URL),
            candidates = listOf(PRIMARY_BASE_URL, FALLBACK_BASE_URL)
        )
        val healthChecker = FakeBackendHealthChecker(healthyBaseUrls = emptySet())

        val viewModel = StartupConnectionViewModel(provider, healthChecker)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartupConnectionUiState.Unavailable, viewModel.uiState.value)
        assertEquals(listOf(PRIMARY_BASE_URL, FALLBACK_BASE_URL), healthChecker.checkedBaseUrls)
    }

    @Test
    fun `maintenance config skips health check and shows maintenance state`() = runTest {
        val provider = FakeBackendConfigProvider(
            config = BackendConfig(
                baseUrl = PRIMARY_BASE_URL,
                maintenanceEnabled = true,
                maintenanceMessage = "Maintenance window"
            ),
            candidates = listOf(PRIMARY_BASE_URL)
        )
        val healthChecker = FakeBackendHealthChecker(healthyBaseUrls = setOf(PRIMARY_BASE_URL))

        val viewModel = StartupConnectionViewModel(provider, healthChecker)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            StartupConnectionUiState.Maintenance("Maintenance window"),
            viewModel.uiState.value
        )
        assertTrue(healthChecker.checkedBaseUrls.isEmpty())
    }

    @Test
    fun `checkAgain forces remote config refresh`() = runTest {
        val provider = FakeBackendConfigProvider(
            config = BackendConfig(PRIMARY_BASE_URL),
            candidates = listOf(PRIMARY_BASE_URL)
        )
        val healthChecker = FakeBackendHealthChecker(healthyBaseUrls = emptySet())
        val viewModel = StartupConnectionViewModel(provider, healthChecker)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.checkAgain()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(false, true), provider.forceRequests)
        assertEquals(StartupConnectionUiState.Unavailable, viewModel.uiState.value)
    }

    private class FakeBackendConfigProvider(
        private var config: BackendConfig,
        private val candidates: List<String>
    ) : BackendConfigProvider {
        val forceRequests = mutableListOf<Boolean>()
        var activatedBaseUrl: String? = null

        override suspend fun refresh(force: Boolean): BackendConfig {
            forceRequests += force
            return config
        }

        override fun currentConfig(): BackendConfig = config

        override fun candidateBaseUrls(): List<String> = candidates

        override fun activateBaseUrl(baseUrl: String) {
            activatedBaseUrl = baseUrl
            config = config.copy(baseUrl = baseUrl)
        }
    }

    private class FakeBackendHealthChecker(
        private val healthyBaseUrls: Set<String>
    ) : BackendHealthChecker {
        val checkedBaseUrls = mutableListOf<String>()

        override suspend fun isHealthy(baseUrl: String): Boolean {
            checkedBaseUrls += baseUrl
            return baseUrl in healthyBaseUrls
        }
    }

    private companion object {
        const val PRIMARY_BASE_URL = "https://primary.example.com/api"
        const val FALLBACK_BASE_URL = "https://fallback.example.com/api"
    }
}
