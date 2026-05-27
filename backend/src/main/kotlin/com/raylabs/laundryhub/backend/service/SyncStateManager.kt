package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class SyncConfig(
    val intervalMinutes: Int = 5,
    val reverseSyncSchedule: ReverseSyncSchedule = ReverseSyncSchedule.MANUAL,
    val masterSourceOfTruth: MasterSourceOfTruth = MasterSourceOfTruth.SHEETS
)

class SyncStateManager {
    private val _config = MutableStateFlow(SyncConfig())
    val config: StateFlow<SyncConfig> = _config.asStateFlow()

    private var _lastSyncTime: String? = null
    private val _lastChangesCount = AtomicInteger(0)
    private val _isSyncing = AtomicBoolean(false)
    private var _lastSyncStatus: String = "UNKNOWN"
    private var _lastSyncError: String? = null

    val lastSyncTime: String? get() = _lastSyncTime
    val lastChangesCount: Int get() = _lastChangesCount.get()
    val isSyncing: Boolean get() = _isSyncing.get()
    val lastSyncStatus: String get() = _lastSyncStatus
    val lastSyncError: String? get() = _lastSyncError

    fun updateInterval(minutes: Int) {
        _config.value = _config.value.copy(intervalMinutes = minutes)
    }

    fun updateReverseSchedule(schedule: ReverseSyncSchedule) {
        _config.value = _config.value.copy(reverseSyncSchedule = schedule)
    }

    fun updateMasterSourceOfTruth(source: MasterSourceOfTruth) {
        _config.value = _config.value.copy(masterSourceOfTruth = source)
    }

    fun recordSync(changesCount: Int, status: String = "SUCCESS") {
        if (changesCount > 0) {
            _lastChangesCount.addAndGet(changesCount)
        }
        _lastSyncStatus = status
        _lastSyncError = null
        _lastSyncTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    fun recordSyncFailure(error: String? = null) {
        _lastSyncStatus = "FAILED"
        _lastSyncError = error
        _lastSyncTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    fun setSyncing(syncing: Boolean) {
        _isSyncing.set(syncing)
    }
}
