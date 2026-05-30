package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

interface SheetsPushJob {
    suspend fun processUnsyncedAll(): Int
}

class SheetsPushScheduler(
    private val batchSyncJob: SheetsPushJob?,
    private val syncStateManager: SyncStateManager,
    private val debounceMillis: Long = configuredDefaultDebounceMillis(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val logger = LoggerFactory.getLogger(SheetsPushScheduler::class.java)
    private val scheduled = AtomicBoolean(false)
    private var scheduledJob: Job? = null
    private var _nextScheduledPushTime: String? = null

    val nextScheduledPushTime: String? get() = _nextScheduledPushTime

    fun requestPush(reason: String) {
        if (batchSyncJob == null) {
            logger.info("Sheets push not scheduled for $reason: sync job is not configured.")
            return
        }
        if (syncStateManager.config.value.masterSourceOfTruth != MasterSourceOfTruth.SUPABASE) {
            logger.info("Sheets push not scheduled for $reason: App Database is not the configured master source.")
            return
        }

        if (!scheduled.compareAndSet(false, true)) {
            logger.info("Sheets push already scheduled. Coalescing $reason into pending batch.")
            return
        }

        _nextScheduledPushTime = LocalDateTime.now()
            .plusNanos(debounceMillis * 1_000_000L)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        logger.info("Sheets push scheduled in ${debounceMillis}ms for $reason.")

        scheduledJob = scope.launch {
            delay(debounceMillis)
            flushScheduled()
        }
    }

    fun triggerNow(reason: String) {
        if (batchSyncJob == null) return
        if (syncStateManager.config.value.masterSourceOfTruth != MasterSourceOfTruth.SUPABASE) return
        if (!scheduled.compareAndSet(false, true)) return
        _nextScheduledPushTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        scheduledJob = scope.launch { flushScheduled(reason) }
    }

    private suspend fun flushScheduled(reason: String = "debounced changes") {
        scheduled.set(false)
        _nextScheduledPushTime = null

        if (syncStateManager.isSyncing) {
            requestPush("sync already running; retry $reason")
            return
        }

        syncStateManager.setSyncing(true)
        try {
            val count = batchSyncJob?.processUnsyncedAll() ?: 0
            syncStateManager.recordSync(count, "SUCCESS")
            logger.info("Sheets push completed for $reason. Synced $count changes.")
        } catch (e: Exception) {
            logger.error("Sheets push failed for $reason: ${e.message}")
            syncStateManager.recordSyncFailure(e.message)
        } finally {
            syncStateManager.setSyncing(false)
        }
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS: Long = 3_000
        const val DEBOUNCE_ENV = "SHEETS_PUSH_DEBOUNCE_MILLIS"

        internal fun configuredDefaultDebounceMillis(): Long {
            return parseDebounceMillis(System.getenv(DEBOUNCE_ENV))
        }

        internal fun parseDebounceMillis(rawValue: String?): Long {
            return rawValue
                ?.trim()
                ?.toLongOrNull()
                ?.takeIf { it >= 0L }
                ?: DEFAULT_DEBOUNCE_MILLIS
        }
    }
}
