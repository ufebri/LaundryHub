package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

data class SyncConfig(
    val intervalMinutes: Int = 15,
    val reverseSyncSchedule: ReverseSyncSchedule = ReverseSyncSchedule.DEFAULT_23
)

class SyncStateManager {
    private val _config = MutableStateFlow(SyncConfig())
    val config: StateFlow<SyncConfig> = _config.asStateFlow()

    private var _lastSyncTime: String? = null
    private val _lastChangesCount = AtomicInteger(0)

    val lastSyncTime: String? get() = _lastSyncTime
    val lastChangesCount: Int get() = _lastChangesCount.get()

    fun updateInterval(minutes: Int) {
        _config.value = _config.value.copy(intervalMinutes = minutes)
    }

    fun updateReverseSchedule(schedule: ReverseSyncSchedule) {
        _config.value = _config.value.copy(reverseSyncSchedule = schedule)
    }

    fun recordSync(changesCount: Int) {
        if (changesCount > 0) {
            _lastChangesCount.addAndGet(changesCount)
        }
        _lastSyncTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
