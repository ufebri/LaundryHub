package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.DeviceTokenRepository
import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.backend.db.repository.PackageRepository
import com.raylabs.laundryhub.backend.db.repository.SummaryRepository
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SheetsReverseSyncJobTest {

    private val orderRepository: OrderRepository = mock()
    private val outcomeRepository: OutcomeRepository = mock()
    private val packageRepository: PackageRepository = mock()
    private val grossRepository: GrossRepository = mock()
    private val summaryRepository: SummaryRepository = mock()
    private val syncService: SheetsSyncService = mock()
    private val syncStateManager = SyncStateManager()
    private val deviceTokenRepository: DeviceTokenRepository = mock()
    private val fcmNotificationService: FcmNotificationService = mock()
    private val spreadsheetId = "test-reverse-spreadsheet-id"

    private fun createJob(scope: kotlinx.coroutines.CoroutineScope) = SheetsReverseSyncJob(
        orderRepository = orderRepository,
        outcomeRepository = outcomeRepository,
        packageRepository = packageRepository,
        grossRepository = grossRepository,
        summaryRepository = summaryRepository,
        syncService = syncService,
        spreadsheetId = spreadsheetId,
        syncStateManager = syncStateManager,
        deviceTokenRepository = deviceTokenRepository,
        fcmNotificationService = fcmNotificationService,
        scope = scope
    )

    private fun dummyOrder(id: String) = OrderData(
        orderId = id,
        name = "Customer $id",
        phoneNumber = "0812345678",
        packageName = "Standard",
        priceKg = "10000",
        totalPrice = "20000",
        paidStatus = "unpaid",
        paymentMethod = "cash",
        remark = "quick",
        weight = "2.0",
        orderDate = "2026-06-01",
        dueDate = "2"
    )

    private fun dummyOutcome(id: String) = OutcomeData(
        id = id,
        date = "2026-06-01",
        purpose = "Soap",
        price = "5000",
        remark = "clean",
        payment = "cash"
    )

    private fun dummyPackage(name: String) = PackageData(
        id = 1,
        price = "15000",
        name = name,
        duration = "24",
        unit = "Hours"
    )

    private fun dummyGross(month: String) = GrossData(
        id = 1,
        month = month,
        totalNominal = "Rp1.000.000",
        orderCount = "10",
        tax = "0"
    )

    @Test
    fun testPullOrdersFromSheets() = runTest {
        val job = createJob(this)
        val orders = listOf(dummyOrder("1"), dummyOrder("2"))

        whenever(syncService.fetchOrdersFromSheet(spreadsheetId)).thenReturn(orders)
        whenever(orderRepository.upsert(any())).thenReturn(true)

        val count = job.pullOrdersFromSheets()
        assertEquals(2, count)
        verify(orderRepository, times(2)).upsert(any())

        // Empty branch
        whenever(syncService.fetchOrdersFromSheet(spreadsheetId)).thenReturn(emptyList())
        assertEquals(0, job.pullOrdersFromSheets())
    }

    @Test
    fun testPullOutcomesFromSheets() = runTest {
        val job = createJob(this)
        val outcomes = listOf(dummyOutcome("1"))

        whenever(syncService.fetchOutcomesFromSheet(spreadsheetId)).thenReturn(outcomes)
        whenever(outcomeRepository.upsert(any())).thenReturn(true)

        val count = job.pullOutcomesFromSheets()
        assertEquals(1, count)
        verify(outcomeRepository).upsert(any())

        // Empty branch
        whenever(syncService.fetchOutcomesFromSheet(spreadsheetId)).thenReturn(emptyList())
        assertEquals(0, job.pullOutcomesFromSheets())
    }

    @Test
    fun testPullPackagesFromSheets() = runTest {
        val job = createJob(this)
        val packages = listOf(dummyPackage("Premium"))

        whenever(syncService.fetchPackagesFromSheet(spreadsheetId)).thenReturn(packages)
        whenever(packageRepository.upsert(any())).thenReturn(true)

        val count = job.pullPackagesFromSheets()
        assertEquals(1, count)
        verify(packageRepository).upsert(any())

        // Empty branch
        whenever(syncService.fetchPackagesFromSheet(spreadsheetId)).thenReturn(emptyList())
        assertEquals(0, job.pullPackagesFromSheets())
    }

    @Test
    fun testPullGrossFromSheets() = runTest {
        val job = createJob(this)
        val gross = listOf(dummyGross("Maret 2026"))

        whenever(syncService.fetchGrossFromSheet(spreadsheetId)).thenReturn(gross)
        whenever(grossRepository.upsert(any())).thenReturn(true)

        val count = job.pullGrossFromSheets()
        assertEquals(1, count)
        verify(grossRepository).upsert(any())

        // Empty branch
        whenever(syncService.fetchGrossFromSheet(spreadsheetId)).thenReturn(emptyList())
        assertEquals(0, job.pullGrossFromSheets())
    }

    @Test
    fun testPullSummaryFromSheets() = runTest {
        val job = createJob(this)
        val summaries = listOf(
            SpreadsheetData(key = "key1", value = "val1")
        )

        whenever(syncService.fetchSummaryFromSheet(spreadsheetId)).thenReturn(summaries)
        whenever(summaryRepository.upsert(any())).thenReturn(true)

        val count = job.pullSummaryFromSheets()
        assertEquals(1, count)
        verify(summaryRepository).upsert(any())

        // Empty branch
        whenever(syncService.fetchSummaryFromSheet(spreadsheetId)).thenReturn(emptyList())
        assertEquals(0, job.pullSummaryFromSheets())
    }

    @Test
    fun testProcessReverseSync() = runTest {
        val job = createJob(this)

        whenever(syncService.fetchOrdersFromSheet(spreadsheetId)).thenReturn(listOf(dummyOrder("1")))
        whenever(syncService.fetchOutcomesFromSheet(spreadsheetId)).thenReturn(listOf(dummyOutcome("2")))
        whenever(syncService.fetchPackagesFromSheet(spreadsheetId)).thenReturn(listOf(dummyPackage("3")))
        whenever(syncService.fetchGrossFromSheet(spreadsheetId)).thenReturn(listOf(dummyGross("4")))
        whenever(syncService.fetchSummaryFromSheet(spreadsheetId)).thenReturn(listOf(SpreadsheetData(key = "5", value = "v")))

        whenever(orderRepository.upsert(any())).thenReturn(true)
        whenever(outcomeRepository.upsert(any())).thenReturn(true)
        whenever(packageRepository.upsert(any())).thenReturn(true)
        whenever(grossRepository.upsert(any())).thenReturn(true)
        whenever(summaryRepository.upsert(any())).thenReturn(true)

        val count = job.processReverseSync()
        assertEquals(5, count)
    }

    @Test
    fun testJobLoopHandlesExceptions() = runTest {
        try {
            val job = createJob(this)
            
            whenever(syncService.fetchOrdersFromSheet(spreadsheetId)).doThrow(RuntimeException("API unavailable"))

            job.start()
            runCurrent()
            
            // Verify sync states
            assertTrue(syncStateManager.lastSyncStatus == "UNKNOWN" || syncStateManager.lastSyncStatus == "FAILED")
        } finally {
            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testIsTimeToRunViaReflection() {
        val job = createJob(kotlinx.coroutines.GlobalScope)
        val method = SheetsReverseSyncJob::class.java.getDeclaredMethod("isTimeToRun", List::class.java)
        method.isAccessible = true

        val zoneId = java.time.ZoneId.of("Asia/Jakarta")
        val now = java.time.LocalDateTime.now(zoneId)
        val currentHour = now.hour
        val currentMinute = now.minute

        if (currentMinute != 0) {
            val result = method.invoke(job, listOf(currentHour)) as Boolean
            assertEquals(false, result)
        } else {
            val result1 = method.invoke(job, listOf(currentHour)) as Boolean
            assertEquals(true, result1)
            
            val result2 = method.invoke(job, listOf(currentHour)) as Boolean
            assertEquals(false, result2)
        }

        val hourNotInList = (currentHour + 1) % 24
        val result3 = method.invoke(job, listOf(hourNotInList)) as Boolean
        assertEquals(false, result3)
    }

    @Test
    fun testJobLoopWithNonManualSchedule() = runTest {
        try {
            val job = createJob(this)
            syncStateManager.updateReverseSchedule(com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule.DEFAULT_23)
            job.start()
            runCurrent()
            assertEquals("UNKNOWN", syncStateManager.lastSyncStatus)
        } finally {
            coroutineContext.cancelChildren()
        }
    }
}
