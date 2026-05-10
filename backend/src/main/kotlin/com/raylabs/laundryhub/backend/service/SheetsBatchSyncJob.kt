package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.backend.db.repository.PackageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class SheetsBatchSyncJob(
    private val orderRepository: OrderRepository,
    private val outcomeRepository: OutcomeRepository,
    private val packageRepository: PackageRepository,
    private val syncService: SheetsSyncService,
    private val spreadsheetId: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val logger = LoggerFactory.getLogger(SheetsBatchSyncJob::class.java)
    private val SYNC_INTERVAL_MS = 15L * 60 * 1000

    fun start() {
        scope.launch {
            logger.info("Background Sync Job Started. Waking up every 15 minutes...")
            while (isActive) {
                try {
                    processUnsyncedOrders()
                    processUnsyncedOutcomes()
                    processUnsyncedPackages()
                } catch (e: Exception) {
                    logger.error("Background Sync Error: ${e.message}")
                }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private suspend fun processUnsyncedOrders() {
        val unsyncedOrders = orderRepository.getUnsyncedOrders()

        if (unsyncedOrders.isEmpty()) {
            logger.info("No unsynced orders found. Sync job skipped.")
            return 
        }

        logger.info("Found ${unsyncedOrders.size} unsynced orders. Preparing smart sync...")

        var successCount = 0
        for (order in unsyncedOrders) {
            val synced = syncService.syncOrder(spreadsheetId, order)
            if (synced) {
                orderRepository.markAsSynced(listOf(order.orderId))
                successCount++
            }
        }

        if (successCount > 0) {
            logger.info("Successfully synced $successCount orders to Google Sheets.")
        }
    }

    private suspend fun processUnsyncedOutcomes() {
        val unsyncedOutcomes = outcomeRepository.getUnsyncedOutcomes()

        if (unsyncedOutcomes.isEmpty()) {
            logger.info("No unsynced outcomes found. Sync job skipped.")
            return
        }

        logger.info("Found ${unsyncedOutcomes.size} unsynced outcomes. Preparing smart sync...")

        var successCount = 0
        for (outcome in unsyncedOutcomes) {
            val synced = syncService.syncOutcome(spreadsheetId, outcome)
            if (synced) {
                outcomeRepository.markAsSynced(listOf(outcome.id))
                successCount++
            }
        }

        if (successCount > 0) {
            logger.info("Successfully synced $successCount outcomes to Google Sheets.")
        }
    }

    private suspend fun processUnsyncedPackages() {
        val unsyncedPackages = packageRepository.getUnsyncedPackages()

        if (unsyncedPackages.isEmpty()) {
            logger.info("No unsynced packages found. Sync job skipped.")
            return
        }

        logger.info("Found ${unsyncedPackages.size} unsynced packages. Preparing smart sync...")

        var successCount = 0
        for (pkg in unsyncedPackages) {
            val synced = syncService.syncPackage(spreadsheetId, pkg)
            if (synced) {
                packageRepository.markAsSynced(listOf(pkg.name))
                successCount++
            }
        }

        if (successCount > 0) {
            logger.info("Successfully synced $successCount packages to Google Sheets.")
        }
    }
}
