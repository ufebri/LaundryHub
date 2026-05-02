package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SheetsBatchSyncJob(
    private val orderRepository: OrderRepository,
    private val syncService: SheetsSyncService,
    private val spreadsheetId: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val SYNC_INTERVAL_MS = 15L * 60 * 1000

    fun start() {
        scope.launch {
            println("Background Sync Job Started. Waking up every 15 minutes...")
            while (isActive) {
                try {
                    processUnsyncedOrders()
                } catch (e: Exception) {
                    println("Background Sync Error: \${e.message}")
                }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private suspend fun processUnsyncedOrders() {
        val unsyncedOrders = orderRepository.getUnsyncedOrders()

        if (unsyncedOrders.isEmpty()) {
            println("No unsynced orders found. Sync job skipped.")
            return 
        }

        println("Found \${unsyncedOrders.size} unsynced orders. Preparing smart sync...")

        var successCount = 0
        for (order in unsyncedOrders) {
            val synced = syncService.syncOrder(spreadsheetId, order)
            if (synced) {
                orderRepository.markAsSynced(listOf(order.orderId))
                successCount++
            }
        }

        if (successCount > 0) {
            println("Successfully synced \$successCount orders to Google Sheets.")
        }
    }
}
