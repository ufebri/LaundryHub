package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.DeviceTokenRepository
import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.backend.db.repository.PackageRepository
import com.raylabs.laundryhub.backend.db.repository.SummaryRepository
import com.raylabs.laundryhub.backend.db.repository.SyncDeleteEventRepository
import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
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
    private val grossRepository: GrossRepository,
    private val summaryRepository: SummaryRepository,
    private val syncDeleteEventRepository: SyncDeleteEventRepository,
    private val syncService: SheetsSyncService,
    private val spreadsheetId: String,
    private val syncStateManager: SyncStateManager,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val fcmNotificationService: FcmNotificationService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : SheetsPushJob {
    private val logger = LoggerFactory.getLogger(SheetsBatchSyncJob::class.java)

    fun start() {
        scope.launch {
            logger.info("Background Sync Job Started.")
            while (isActive) {
                val intervalMinutes = syncStateManager.config.value.intervalMinutes
                logger.info("Sync Job waiting for $intervalMinutes minutes...")
                if (syncStateManager.config.value.masterSourceOfTruth != MasterSourceOfTruth.SUPABASE) {
                    logger.info("Sync job skipped because App Database is not the configured master source.")
                } else if (syncStateManager.isSyncing) {
                    logger.info("Sync job skipped because another sync is already running.")
                } else {
                    syncStateManager.setSyncing(true)
                    try {
                        val count = processUnsyncedAll()
                        syncStateManager.recordSync(count, "SUCCESS")
                    } catch (e: Exception) {
                        logger.error("Background Sync Error: ${e.message}")
                        syncStateManager.recordSyncFailure(e.message)
                    } finally {
                        syncStateManager.setSyncing(false)
                    }
                }
                delay(intervalMinutes * 60 * 1000L)
            }
        }
    }

    override suspend fun processUnsyncedAll(): Int {
        val deleteCount = processPendingDeletes()
        val oCount = processUnsyncedOrders()
        val outCount = processUnsyncedOutcomes()
        val pkgCount = processUnsyncedPackages()
        return deleteCount + oCount + outCount + pkgCount
    }

    suspend fun pendingPushCount(): Int {
        return orderRepository.getUnsyncedOrders().size +
            outcomeRepository.getUnsyncedOutcomes().size +
            packageRepository.getUnsyncedPackages().size
    }

    suspend fun pendingDeleteCount(): Int {
        return syncDeleteEventRepository.getPending().size
    }

    suspend fun processUnsyncedOrders(): Int {
        val unsyncedOrders = orderRepository.getUnsyncedOrders()

        if (unsyncedOrders.isEmpty()) {
            logger.info("No unsynced orders found. Sync job skipped.")
            return 0
        }

        logger.info("Found ${unsyncedOrders.size} unsynced orders. Preparing smart sync...")

        val verifiedIds = syncService.syncAndVerifyOrdersBatch(spreadsheetId, unsyncedOrders)
        if (verifiedIds.isEmpty()) {
            error("Order sync wrote no verified rows for ${unsyncedOrders.size} pending orders.")
        }
        if (verifiedIds.isNotEmpty()) {
            orderRepository.markAsSynced(verifiedIds)
        }

        if (verifiedIds.isNotEmpty()) {
            logger.info("Successfully synced and verified ${verifiedIds.size} orders to Google Sheets.")
        }
        return verifiedIds.size
    }

    suspend fun processUnsyncedOutcomes(): Int {
        val unsyncedOutcomes = outcomeRepository.getUnsyncedOutcomes()

        if (unsyncedOutcomes.isEmpty()) {
            logger.info("No unsynced outcomes found. Sync job skipped.")
            return 0
        }

        logger.info("Found ${unsyncedOutcomes.size} unsynced outcomes. Preparing smart sync...")

        val verifiedIds = syncService.syncAndVerifyOutcomesBatch(spreadsheetId, unsyncedOutcomes)
        if (verifiedIds.isEmpty()) {
            error("Outcome sync wrote no verified rows for ${unsyncedOutcomes.size} pending outcomes.")
        }
        if (verifiedIds.isNotEmpty()) {
            outcomeRepository.markAsSynced(verifiedIds)
        }

        if (verifiedIds.isNotEmpty()) {
            logger.info("Successfully synced and verified ${verifiedIds.size} outcomes to Google Sheets.")
        }
        return verifiedIds.size
    }

    suspend fun processUnsyncedPackages(): Int {
        val unsyncedPackages = packageRepository.getUnsyncedPackages()

        if (unsyncedPackages.isEmpty()) {
            logger.info("No unsynced packages found. Sync job skipped.")
            return 0
        }

        logger.info("Found ${unsyncedPackages.size} unsynced packages. Preparing smart sync...")

        val verifiedNames = syncService.syncAndVerifyPackagesBatch(spreadsheetId, unsyncedPackages)
        if (verifiedNames.isEmpty()) {
            error("Package sync wrote no verified rows for ${unsyncedPackages.size} pending packages.")
        }
        if (verifiedNames.isNotEmpty()) {
            packageRepository.markAsSynced(verifiedNames)
        }

        if (verifiedNames.isNotEmpty()) {
            logger.info("Successfully synced and verified ${verifiedNames.size} packages to Google Sheets.")
        }
        return verifiedNames.size
    }

    suspend fun processUnsyncedGross(): Int {
        logger.info("Gross DB -> Sheets push skipped because gross is Sheet-owned reporting data.")
        return 0
    }

    suspend fun processUnsyncedSummaries(): Int {
        logger.info("Summary DB -> Sheets push skipped because summary is Sheet-owned reporting data.")
        return 0
    }

    suspend fun processAllOrdersToSheets(): Int {
        val orders = orderRepository.getAll(page = 1, size = SYNC_READ_LIMIT)
        val verifiedIds = syncService.syncAndVerifyOrdersBatch(spreadsheetId, orders)
        if (verifiedIds.isNotEmpty()) {
            orderRepository.markAsSynced(verifiedIds)
        }
        return verifiedIds.size
    }

    suspend fun processAllOutcomesToSheets(): Int {
        val outcomes = outcomeRepository.getAll(page = 1, size = SYNC_READ_LIMIT)
        val verifiedIds = syncService.syncAndVerifyOutcomesBatch(spreadsheetId, outcomes)
        if (verifiedIds.isNotEmpty()) {
            outcomeRepository.markAsSynced(verifiedIds)
        }
        return verifiedIds.size
    }

    suspend fun processAllPackagesToSheets(): Int {
        val packages = packageRepository.getAll()
        val verifiedNames = syncService.syncAndVerifyPackagesBatch(spreadsheetId, packages)
        if (verifiedNames.isNotEmpty()) {
            packageRepository.markAsSynced(verifiedNames)
        }
        return verifiedNames.size
    }

    suspend fun processPendingDeletes(): Int {
        val pendingDeletes = syncDeleteEventRepository.getPending()
        if (pendingDeletes.isEmpty()) {
            logger.info("No pending sheet delete events found. Sync job skipped.")
            return 0
        }

        logger.info("Found ${pendingDeletes.size} pending sheet delete events. Preparing clear batch...")
        val processedIds = syncService.clearDeletedRows(spreadsheetId, pendingDeletes)
        if (processedIds.isNotEmpty()) {
            syncDeleteEventRepository.markProcessed(processedIds)
            logger.info("Successfully cleared ${processedIds.size} deleted rows from Google Sheets.")
        }
        return processedIds.size
    }
}

private const val SYNC_READ_LIMIT = 100_000
