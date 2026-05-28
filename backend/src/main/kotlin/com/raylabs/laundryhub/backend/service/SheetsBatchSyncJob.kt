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
        val grossCount = processUnsyncedGross()
        val summaryCount = processUnsyncedSummaries()
        return deleteCount + oCount + outCount + pkgCount + grossCount + summaryCount
    }

    suspend fun pendingPushCount(): Int {
        return orderRepository.getUnsyncedOrders().size +
            outcomeRepository.getUnsyncedOutcomes().size +
            packageRepository.getUnsyncedPackages().size +
            grossRepository.getUnsyncedGross().size +
            summaryRepository.getUnsyncedSummaries().size
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

        val successCount = syncService.syncOrdersBatch(spreadsheetId, unsyncedOrders)
        if (successCount > 0) {
            orderRepository.markAsSynced(unsyncedOrders.take(successCount).map { it.orderId })
        }

        if (successCount > 0) {
            logger.info("Successfully synced $successCount orders to Google Sheets.")
        }
        return successCount
    }

    suspend fun processUnsyncedOutcomes(): Int {
        val unsyncedOutcomes = outcomeRepository.getUnsyncedOutcomes()

        if (unsyncedOutcomes.isEmpty()) {
            logger.info("No unsynced outcomes found. Sync job skipped.")
            return 0
        }

        logger.info("Found ${unsyncedOutcomes.size} unsynced outcomes. Preparing smart sync...")

        val successCount = syncService.syncOutcomesBatch(spreadsheetId, unsyncedOutcomes)
        if (successCount > 0) {
            outcomeRepository.markAsSynced(unsyncedOutcomes.take(successCount).map { it.id })
        }

        if (successCount > 0) {
            logger.info("Successfully synced $successCount outcomes to Google Sheets.")
        }
        return successCount
    }

    suspend fun processUnsyncedPackages(): Int {
        val unsyncedPackages = packageRepository.getUnsyncedPackages()

        if (unsyncedPackages.isEmpty()) {
            logger.info("No unsynced packages found. Sync job skipped.")
            return 0
        }

        logger.info("Found ${unsyncedPackages.size} unsynced packages. Preparing smart sync...")

        val successCount = syncService.syncPackagesBatch(spreadsheetId, unsyncedPackages)
        if (successCount > 0) {
            packageRepository.markAsSynced(unsyncedPackages.take(successCount).map { it.name })
        }

        if (successCount > 0) {
            logger.info("Successfully synced $successCount packages to Google Sheets.")
        }
        return successCount
    }

    suspend fun processUnsyncedGross(): Int {
        val unsyncedGross = grossRepository.getUnsyncedGross()

        if (unsyncedGross.isEmpty()) {
            logger.info("No unsynced gross rows found. Sync job skipped.")
            return 0
        }

        logger.info("Found ${unsyncedGross.size} unsynced gross rows. Preparing smart sync...")

        val successCount = syncService.syncGrossBatch(spreadsheetId, unsyncedGross)
        if (successCount > 0) {
            grossRepository.markAsSynced(unsyncedGross.take(successCount).map { it.month })
            logger.info("Successfully synced $successCount gross rows to Google Sheets.")
        }
        return successCount
    }

    suspend fun processUnsyncedSummaries(): Int {
        val unsyncedSummaries = summaryRepository.getUnsyncedSummaries()

        if (unsyncedSummaries.isEmpty()) {
            logger.info("No unsynced summary rows found. Sync job skipped.")
            return 0
        }

        logger.info("Found ${unsyncedSummaries.size} unsynced summary rows. Preparing smart sync...")

        val successCount = syncService.syncSummariesBatch(spreadsheetId, unsyncedSummaries)
        if (successCount > 0) {
            summaryRepository.markAsSynced(unsyncedSummaries.take(successCount).map { it.key })
            logger.info("Successfully synced $successCount summary rows to Google Sheets.")
        }
        return successCount
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
