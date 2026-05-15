package com.raylabs.laundryhub.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import com.raylabs.laundryhub.core.domain.model.sheets.SyncConfigUpdateRequest
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncSettingsUiState(
    val lastSyncTime: String? = null,
    val changesCount: Int = 0,
    val autoSyncIntervalMinutes: Int = 5,
    val reverseSyncSchedule: ReverseSyncSchedule = ReverseSyncSchedule.MANUAL,
    val masterSourceOfTruth: MasterSourceOfTruth = MasterSourceOfTruth.SUPABASE,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncStatus: String = "UNKNOWN",
    val lastSyncError: String? = null,
    val pendingPushCount: Int = 0,
    val pendingDeleteCount: Int = 0,
    val nextScheduledPushTime: String? = null,
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
            if (!_uiState.value.isSyncing) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            
            when (val result = repository.getSyncStatus()) {
                is Resource.Success -> {
                    val data = result.data
                    val wasSyncing = _uiState.value.isSyncing
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastSyncTime = data.lastSyncTime,
                            changesCount = data.changesCount,
                            autoSyncIntervalMinutes = data.autoSyncIntervalMinutes,
                            reverseSyncSchedule = data.reverseSyncSchedule,
                            masterSourceOfTruth = data.masterSourceOfTruth,
                            isSyncing = data.isSyncing,
                            lastSyncStatus = data.lastSyncStatus,
                            lastSyncError = data.lastSyncError,
                            pendingPushCount = data.pendingPushCount,
                            pendingDeleteCount = data.pendingDeleteCount,
                            nextScheduledPushTime = data.nextScheduledPushTime
                        )
                    }
                    
                    if (data.isSyncing) {
                        pollSyncStatus()
                    } else if (wasSyncing && !data.isSyncing) {
                        // Sync just finished
                        _uiState.update { it.copy(successMessage = "Sync completed successfully.") }
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, isSyncing = false, errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    private fun pollSyncStatus() {
        viewModelScope.launch {
            delay(3000)
            fetchStatus()
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

    fun updateMasterSourceOfTruth(source: MasterSourceOfTruth) {
        if (_uiState.value.masterSourceOfTruth == source) return

        viewModelScope.launch {
            _uiState.update { it.copy(masterSourceOfTruth = source) }
            val request = SyncConfigUpdateRequest(masterSourceOfTruth = source)
            repository.updateSyncConfig(request)
        }
    }

    fun triggerManualSync() {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null, successMessage = null) }
            when (val result = repository.triggerManualSync()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(successMessage = result.data.message) }
                    pollSyncStatus()
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
