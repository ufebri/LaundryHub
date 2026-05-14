package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.DeviceTokenRepository
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
    private val syncStateManager: SyncStateManager,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val fcmNotificationService: FcmNotificationService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val logger = LoggerFactory.getLogger(SheetsBatchSyncJob::class.java)

    fun start() {
        scope.launch {
            logger.info("Background Sync Job Started.")
            while (isActive) {
                val intervalMinutes = syncStateManager.config.value.intervalMinutes
                logger.info("Sync Job waiting for $intervalMinutes minutes...")
                try {
                    val count = processUnsyncedAll()
                    syncStateManager.recordSync(count, "SUCCESS")
                } catch (e: Exception) {
                    logger.error("Background Sync Error: ${e.message}")
                    syncStateManager.recordSyncFailure()
                }
                delay(intervalMinutes * 60 * 1000L)
            }
        }
    }

    suspend fun processUnsyncedAll(): Int {
        val oCount = processUnsyncedOrders()
        val outCount = processUnsyncedOutcomes()
        val pkgCount = processUnsyncedPackages()
        return oCount + outCount + pkgCount
    }

    private suspend fun processUnsyncedOrders(): Int {
        val unsyncedOrders = orderRepository.getUnsyncedOrders()

        if (unsyncedOrders.isEmpty()) {
            logger.info("No unsynced orders found. Sync job skipped.")
            return 0
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
        return successCount
    }

    private suspend fun processUnsyncedOutcomes(): Int {
        val unsyncedOutcomes = outcomeRepository.getUnsyncedOutcomes()

        if (unsyncedOutcomes.isEmpty()) {
            logger.info("No unsynced outcomes found. Sync job skipped.")
            return 0
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
        return successCount
    }

    private suspend fun processUnsyncedPackages(): Int {
        val unsyncedPackages = packageRepository.getUnsyncedPackages()

        if (unsyncedPackages.isEmpty()) {
            logger.info("No unsynced packages found. Sync job skipped.")
            return 0
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
        return successCount
    }
}

