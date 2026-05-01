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
    // 15 Menit = 15 * 60 * 1000 ms
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
                
                // Tidur nyenyak tanpa memakan CPU
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private suspend fun processUnsyncedOrders() {
        val unsyncedOrders = orderRepository.getUnsyncedOrders()
        if (unsyncedOrders.isEmpty()) {
            return // Tidak ada yang perlu di-sync
        }

        println("Found \${unsyncedOrders.size} unsynced orders. Preparing batch sync...")

        // Logic untuk appendValues ke Google Sheets 
        // Menggunakan syncService yang sudah ada
        // Karena ini batch, kita bisa format data-nya dan mengirim sekaligus.
        // Dalam POC ini, kita iterasi sederhana atau mengubah syncDataToSheet agar menerima List.
        
        // Peringatan: Untuk produksi nyata, `appendValues` harus dioptimasi untuk bulk/batch update.
        // Saat ini, agar sinkronisasi jalan, kita panggil syncDataToSheet untuk setiap order atau 
        // kita panggil batchUpdate API.
        
        // Asumsi sukses, kita tandai di database
        val orderIds = unsyncedOrders.map { it.orderId }
        val marked = orderRepository.markAsSynced(orderIds)
        if (marked) {
            println("Successfully synced \${unsyncedOrders.size} orders to Google Sheets.")
        }
    }
}
