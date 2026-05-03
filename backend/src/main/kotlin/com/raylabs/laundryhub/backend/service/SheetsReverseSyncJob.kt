package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class SheetsReverseSyncJob(
    private val orderRepository: OrderRepository,
    private val syncService: SheetsSyncService,
    private val spreadsheetId: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    fun start() {
        scope.launch {
            println("Reverse Sync Job Initialized. Calculating time until 23:00...")
            while (isActive) {
                try {
                    val delayMillis = calculateDelayUntil(23, 0)
                    println("Reverse Sync will run in \${delayMillis / 1000 / 60} minutes.")
                    delay(delayMillis)

                    // Execute reverse sync
                    processReverseSync()

                    // Wait a bit before calculating the next day's 23:00 to avoid double triggering
                    delay(60_000)
                } catch (e: Exception) {
                    println("Reverse Sync Error: \${e.message}")
                    delay(15L * 60 * 1000) // Retry after 15 mins on error
                }
            }
        }
    }

    private suspend fun processReverseSync() {
        println("Starting Reverse Sync (Sheets -> DB)...")
        val sheetOrders = syncService.fetchOrdersFromSheet(spreadsheetId)
        
        if (sheetOrders.isEmpty()) {
            println("No data found in Google Sheets for reverse sync.")
            return
        }

        var successCount = 0
        for (order in sheetOrders) {
            val isUpserted = orderRepository.upsert(order)
            if (isUpserted) {
                successCount++
            }
        }

        println("Reverse Sync Completed. Upserted \$successCount/\${sheetOrders.size} orders into PostgreSQL.")
    }

    private fun calculateDelayUntil(targetHour: Int, targetMinute: Int): Long {
        val zoneId = ZoneId.of("Asia/Jakarta") // Gunakan timezone lokal (WIB)
        val now = LocalDateTime.now(zoneId)
        var targetTime = now.withHour(targetHour).withMinute(targetMinute).withSecond(0).withNano(0)

        // Jika waktu target sudah lewat hari ini, jadwalkan untuk besok
        if (now.isAfter(targetTime) || now.isEqual(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }

        return ChronoUnit.MILLIS.between(now, targetTime)
    }
}
