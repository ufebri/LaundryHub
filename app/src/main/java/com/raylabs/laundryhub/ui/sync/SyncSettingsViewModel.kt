package com.raylabs.laundryhub.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import com.raylabs.laundryhub.core.domain.model.sheets.SyncConfigUpdateRequest
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncSettingsUiState(
    val lastSyncTime: String? = null,
    val changesCount: Int = 0,
    val autoSyncIntervalMinutes: Int = 15,
    val reverseSyncSchedule: ReverseSyncSchedule = ReverseSyncSchedule.DEFAULT_23,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val repository: LaundryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncSettingsUiState())
    val uiState: StateFlow<SyncSettingsUiState> = _uiState.asStateFlow()

    init {
        fetchStatus()
    }

    fun fetchStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getSyncStatus()) {
                is Resource.Success -> {
                    val data = result.data
                    if (data != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                lastSyncTime = data.lastSyncTime,
                                changesCount = data.changesCount,
                                autoSyncIntervalMinutes = data.autoSyncIntervalMinutes,
                                reverseSyncSchedule = data.reverseSyncSchedule
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    fun updateAutoSyncInterval(minutes: Int) {
        if (_uiState.value.autoSyncIntervalMinutes == minutes) return

        viewModelScope.launch {
            _uiState.update { it.copy(autoSyncIntervalMinutes = minutes) }
            val request = SyncConfigUpdateRequest(autoSyncIntervalMinutes = minutes)
            repository.updateSyncConfig(request)
        }
    }

    fun updateReverseSyncSchedule(schedule: ReverseSyncSchedule) {
        if (_uiState.value.reverseSyncSchedule == schedule) return

        viewModelScope.launch {
            _uiState.update { it.copy(reverseSyncSchedule = schedule) }
            val request = SyncConfigUpdateRequest(reverseSyncSchedule = schedule)
            repository.updateSyncConfig(request)
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null, successMessage = null) }
            when (val result = repository.triggerManualSync()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSyncing = false, successMessage = result.data?.message) }
                    fetchStatus() // Refresh the status to get the latest count and time
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSyncing = false, errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
