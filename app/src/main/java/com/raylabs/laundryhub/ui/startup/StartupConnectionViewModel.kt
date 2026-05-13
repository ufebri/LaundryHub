package com.raylabs.laundryhub.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.config.BackendConfigProvider
import com.raylabs.laundryhub.core.domain.config.BackendHealthChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupConnectionViewModel @Inject constructor(
    private val backendConfigProvider: BackendConfigProvider,
    private val backendHealthChecker: BackendHealthChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow<StartupConnectionUiState>(
        StartupConnectionUiState.Checking
    )
    val uiState: StateFlow<StartupConnectionUiState> = _uiState.asStateFlow()

    private var checkJob: Job? = null

    init {
        checkConnection(forceRemote = false, isManualRetry = false)
    }

    fun checkAgain() {
        checkConnection(forceRemote = true, isManualRetry = true)
    }

    private fun checkConnection(forceRemote: Boolean, isManualRetry: Boolean) {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _uiState.value = if (isManualRetry) {
                StartupConnectionUiState.Retrying
            } else {
                StartupConnectionUiState.Checking
            }

            val config = backendConfigProvider.refresh(force = forceRemote)
            if (config.maintenanceEnabled) {
                _uiState.value = StartupConnectionUiState.Maintenance(config.maintenanceMessage)
                return@launch
            }

            val healthyBaseUrl = findFirstHealthyBaseUrl()
            if (healthyBaseUrl == null) {
                _uiState.value = StartupConnectionUiState.Unavailable
                return@launch
            }

            backendConfigProvider.activateBaseUrl(healthyBaseUrl)
            _uiState.value = StartupConnectionUiState.Ready
        }
    }

    private suspend fun findFirstHealthyBaseUrl(): String? {
        return backendConfigProvider
            .candidateBaseUrls()
            .firstOrNullSuspend { backendHealthChecker.isHealthy(it) }
    }
}

private suspend fun <T> Iterable<T>.firstOrNullSuspend(predicate: suspend (T) -> Boolean): T? {
    for (item in this) {
        if (predicate(item)) return item
    }
    return null
}
