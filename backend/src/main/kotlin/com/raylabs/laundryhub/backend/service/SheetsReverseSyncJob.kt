package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.backend.db.repository.PackageRepository
import com.raylabs.laundryhub.backend.db.repository.SummaryRepository
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId

class SheetsReverseSyncJob(
    private val orderRepository: OrderRepository,
    private val outcomeRepository: OutcomeRepository,
    private val packageRepository: PackageRepository,
    private val grossRepository: GrossRepository,
    private val summaryRepository: SummaryRepository,
    private val syncService: SheetsSyncService,
    private val spreadsheetId: String,
    private val syncStateManager: SyncStateManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val logger = LoggerFactory.getLogger(SheetsReverseSyncJob::class.java)
    private var lastRunDayHour: String = ""

    fun start() {
        scope.launch {
            logger.info("Reverse Sync Job Initialized.")
            while (isActive) {
                try {
                    val schedule = syncStateManager.config.value.reverseSyncSchedule
                    if (schedule != ReverseSyncSchedule.MANUAL) {
                        if (isTimeToRun(schedule.hours)) {
                            logger.info("It is time to run reverse sync. Triggering...")
                            syncStateManager.setSyncing(true)
                            try {
                                val count = processReverseSync()
                                if (count > 0) {
                                    syncStateManager.recordSync(count)
                                }
                            } finally {
                                syncStateManager.setSyncing(false)
                            }
                        }
                    }
                    delay(60_000) // Check every minute
                } catch (e: Exception) {
                    logger.error("Reverse Sync Error: ${e.message}")
                    syncStateManager.setSyncing(false)
                    delay(60_000)
                }
            }
        }
    }

    suspend fun processReverseSync(): Int {
        logger.info("Starting Reverse Sync (Sheets -> DB)...")
        var successCount = 0

        val sheetOrders = syncService.fetchOrdersFromSheet(spreadsheetId)
        if (sheetOrders.isNotEmpty()) {
            for (order in sheetOrders) {
                if (orderRepository.upsert(order)) successCount++
            }
        }

        val sheetOutcomes = syncService.fetchOutcomesFromSheet(spreadsheetId)
        if (sheetOutcomes.isNotEmpty()) {
            for (outcome in sheetOutcomes) {
                if (outcomeRepository.upsert(outcome)) successCount++
            }
        }

        val sheetPackages = syncService.fetchPackagesFromSheet(spreadsheetId)
        if (sheetPackages.isNotEmpty()) {
            for (pkg in sheetPackages) {
                if (packageRepository.upsert(pkg)) successCount++
            }
        }

        val sheetGross = syncService.fetchGrossFromSheet(spreadsheetId)
        if (sheetGross.isNotEmpty()) {
            for (gross in sheetGross) {
                if (grossRepository.upsert(gross)) successCount++
            }
        }

        val sheetSummary = syncService.fetchSummaryFromSheet(spreadsheetId)
        if (sheetSummary.isNotEmpty()) {
            for (summary in sheetSummary) {
                if (summaryRepository.upsert(summary)) successCount++
            }
        }

        logger.info("Reverse Sync Completed. Upserted $successCount total items into PostgreSQL.")
        return successCount
    }

    private fun isTimeToRun(hours: List<Int>): Boolean {
        val zoneId = ZoneId.of("Asia/Jakarta")
        val now = LocalDateTime.now(zoneId)
        val currentHour = now.hour
        val currentMinute = now.minute
        val currentDayHour = "${now.dayOfYear}-$currentHour"
        
        // Ensure it runs at minute 0 of the configured hour
        if (currentMinute == 0 && hours.contains(currentHour) && lastRunDayHour != currentDayHour) {
            lastRunDayHour = currentDayHour
            return true
        }
        return false
    }
}


