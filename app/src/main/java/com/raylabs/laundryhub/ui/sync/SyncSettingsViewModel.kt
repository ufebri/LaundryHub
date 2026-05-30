package com.raylabs.laundryhub.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewRequest
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunRequest
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatus
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatusResponse
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
    val masterSourceOfTruth: MasterSourceOfTruth = MasterSourceOfTruth.SHEETS,
    val selectedSourceOfTruth: MasterSourceOfTruth = MasterSourceOfTruth.SHEETS,
    val isLoading: Boolean = false,
    val isCheckingDifferences: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncStatus: String = "UNKNOWN",
    val lastSyncError: String? = null,
    val pendingPushCount: Int = 0,
    val pendingDeleteCount: Int = 0,
    val dataDifferenceCount: Int = 0,
    val reportingDifferenceCount: Int = 0,
    val nextScheduledPushTime: String? = null,
    val syncPreview: SyncPreviewResponse? = null,
    val activeRun: SyncRunStatusResponse? = null,
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
                            selectedSourceOfTruth = if (data.masterSourceOfTruth == MasterSourceOfTruth.BOTH) {
                                MasterSourceOfTruth.SHEETS
                            } else {
                                data.masterSourceOfTruth
                            },
                            isSyncing = data.isSyncing,
                            lastSyncStatus = data.lastSyncStatus,
                            lastSyncError = data.lastSyncError,
                            pendingPushCount = data.pendingPushCount,
                            pendingDeleteCount = data.pendingDeleteCount,
                            dataDifferenceCount = data.dataDifferenceCount,
                            reportingDifferenceCount = data.reportingDifferenceCount,
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

    fun selectSourceOfTruth(source: MasterSourceOfTruth) {
        if (source == MasterSourceOfTruth.BOTH) return
        _uiState.update {
            it.copy(
                selectedSourceOfTruth = source,
                syncPreview = null,
                activeRun = null,
                successMessage = null,
                errorMessage = null
            )
        }
    }

    fun checkDifferences() {
        if (_uiState.value.isCheckingDifferences || _uiState.value.isSyncing) return

        viewModelScope.launch {
            val selectedSource = _uiState.value.selectedSourceOfTruth
            _uiState.update {
                it.copy(
                    isCheckingDifferences = true,
                    syncPreview = null,
                    activeRun = null,
                    errorMessage = null,
                    successMessage = null
                )
            }
            when (val result = repository.previewSync(SyncPreviewRequest(selectedSource))) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isCheckingDifferences = false,
                            syncPreview = result.data,
                            successMessage = if (result.data.totalDifferences == 0) {
                                "Data is already in sync."
                            } else {
                                null
                            }
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isCheckingDifferences = false, errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    fun confirmSyncNow() {
        val preview = _uiState.value.syncPreview ?: return
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncPreview = null,
                    activeRun = null,
                    errorMessage = null,
                    successMessage = null
                )
            }
            when (val result = repository.startSyncRun(SyncRunRequest(preview.previewId, _uiState.value.selectedSourceOfTruth))) {
                is Resource.Success -> pollRunStatus(result.data.runId)
                is Resource.Error -> {
                    _uiState.update { it.copy(isSyncing = false, errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    fun dismissPreview() {
        _uiState.update { it.copy(syncPreview = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private suspend fun pollRunStatus(runId: String) {
        while (true) {
            when (val result = repository.getSyncRunStatus(runId)) {
                is Resource.Success -> {
                    val run = result.data
                    _uiState.update {
                        it.copy(
                            activeRun = run,
                            isSyncing = run.status.isActive(),
                            successMessage = when (run.status) {
                                SyncRunStatus.SUCCEEDED -> "Sync completed successfully."
                                SyncRunStatus.PARTIAL -> "Sync completed with remaining differences."
                                else -> it.successMessage
                            },
                            errorMessage = if (run.status == SyncRunStatus.FAILED) {
                                run.lastError ?: "Sync failed."
                            } else {
                                it.errorMessage
                            }
                        )
                    }
                    if (!run.status.isActive()) {
                        fetchStatus()
                        return
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSyncing = false, errorMessage = result.message) }
                    return
                }
                else -> {}
            }
            delay(1500)
        }
    }
}

private fun SyncRunStatus.isActive(): Boolean {
    return this == SyncRunStatus.PENDING || this == SyncRunStatus.RUNNING
}
