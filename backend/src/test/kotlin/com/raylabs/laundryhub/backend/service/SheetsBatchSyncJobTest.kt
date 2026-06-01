package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.DeviceTokenRepository
import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.backend.db.repository.PackageRepository
import com.raylabs.laundryhub.backend.db.repository.SummaryRepository
import com.raylabs.laundryhub.backend.db.repository.SyncDeleteEvent
import com.raylabs.laundryhub.backend.db.repository.SyncDeleteEventRepository
import com.raylabs.laundryhub.backend.db.repository.SyncEntityType
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class SheetsBatchSyncJobTest {

    private val orderRepository: OrderRepository = mock()
    private val outcomeRepository: OutcomeRepository = mock()
    private val packageRepository: PackageRepository = mock()
    private val grossRepository: GrossRepository = mock()
    private val summaryRepository: SummaryRepository = mock()
    private val syncDeleteEventRepository: SyncDeleteEventRepository = mock()
    private val syncService: SheetsSyncService = mock()
    private val syncStateManager = SyncStateManager()
    private val deviceTokenRepository: DeviceTokenRepository = mock()
    private val fcmNotificationService: FcmNotificationService = mock()
    private val spreadsheetId = "test-spreadsheet-id"

    private fun createJob(scope: kotlinx.coroutines.CoroutineScope) = SheetsBatchSyncJob(
        orderRepository = orderRepository,
        outcomeRepository = outcomeRepository,
        packageRepository = packageRepository,
        grossRepository = grossRepository,
        summaryRepository = summaryRepository,
        syncDeleteEventRepository = syncDeleteEventRepository,
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
    fun testPendingCounts() = runTest {
        val job = createJob(this)

        whenever(orderRepository.getUnsyncedOrders()).thenReturn(listOf(mock(), mock()))
        whenever(outcomeRepository.getUnsyncedOutcomes()).thenReturn(listOf(mock()))
        whenever(packageRepository.getUnsyncedPackages()).thenReturn(emptyList())
        whenever(syncDeleteEventRepository.getPending()).thenReturn(listOf(mock(), mock(), mock()))

        assertEquals(3, job.pendingPushCount())
        assertEquals(3, job.pendingDeleteCount())
    }

    @Test
    fun testProcessUnsyncedOrdersHappyPath() = runTest {
        val job = createJob(this)
        val orders = listOf(dummyOrder("1"), dummyOrder("2"))

        whenever(orderRepository.getUnsyncedOrders()).thenReturn(orders)
        whenever(syncService.syncAndVerifyOrdersBatch(spreadsheetId, orders)).thenReturn(listOf("1", "2"))

        val result = job.processUnsyncedOrders()
        assertEquals(2, result)
        verify(orderRepository).markAsSynced(listOf("1", "2"))
    }

    @Test
    fun testProcessUnsyncedOrdersEmpty() = runTest {
        val job = createJob(this)
        whenever(orderRepository.getUnsyncedOrders()).thenReturn(emptyList())

        val result = job.processUnsyncedOrders()
        assertEquals(0, result)
    }

    @Test
    fun testProcessUnsyncedOrdersFailure() = runTest {
        val job = createJob(this)
        val orders = listOf(dummyOrder("1"))

        whenever(orderRepository.getUnsyncedOrders()).thenReturn(orders)
        whenever(syncService.syncAndVerifyOrdersBatch(spreadsheetId, orders)).thenReturn(emptyList())

        assertFailsWith<IllegalStateException> {
            job.processUnsyncedOrders()
        }
    }

    @Test
    fun testProcessUnsyncedOutcomesHappyPath() = runTest {
        val job = createJob(this)
        val outcomes = listOf(dummyOutcome("1"), dummyOutcome("2"))

        whenever(outcomeRepository.getUnsyncedOutcomes()).thenReturn(outcomes)
        whenever(syncService.syncAndVerifyOutcomesBatch(spreadsheetId, outcomes)).thenReturn(listOf("1", "2"))

        val result = job.processUnsyncedOutcomes()
        assertEquals(2, result)
        verify(outcomeRepository).markAsSynced(listOf("1", "2"))
    }

    @Test
    fun testProcessUnsyncedOutcomesEmpty() = runTest {
        val job = createJob(this)
        whenever(outcomeRepository.getUnsyncedOutcomes()).thenReturn(emptyList())

        val result = job.processUnsyncedOutcomes()
        assertEquals(0, result)
    }

    @Test
    fun testProcessUnsyncedOutcomesFailure() = runTest {
        val job = createJob(this)
        val outcomes = listOf(dummyOutcome("1"))

        whenever(outcomeRepository.getUnsyncedOutcomes()).thenReturn(outcomes)
        whenever(syncService.syncAndVerifyOutcomesBatch(spreadsheetId, outcomes)).thenReturn(emptyList())

        assertFailsWith<IllegalStateException> {
            job.processUnsyncedOutcomes()
        }
    }

    @Test
    fun testProcessUnsyncedPackagesHappyPath() = runTest {
        val job = createJob(this)
        val packages = listOf(dummyPackage("Express"), dummyPackage("Standard"))

        whenever(packageRepository.getUnsyncedPackages()).thenReturn(packages)
        whenever(syncService.syncAndVerifyPackagesBatch(spreadsheetId, packages)).thenReturn(listOf("Express", "Standard"))

        val result = job.processUnsyncedPackages()
        assertEquals(2, result)
        verify(packageRepository).markAsSynced(listOf("Express", "Standard"))
    }

    @Test
    fun testProcessUnsyncedPackagesEmpty() = runTest {
        val job = createJob(this)
        whenever(packageRepository.getUnsyncedPackages()).thenReturn(emptyList())

        val result = job.processUnsyncedPackages()
        assertEquals(0, result)
    }

    @Test
    fun testProcessUnsyncedPackagesFailure() = runTest {
        val job = createJob(this)
        val packages = listOf(dummyPackage("Express"))

        whenever(packageRepository.getUnsyncedPackages()).thenReturn(packages)
        whenever(syncService.syncAndVerifyPackagesBatch(spreadsheetId, packages)).thenReturn(emptyList())

        assertFailsWith<IllegalStateException> {
            job.processUnsyncedPackages()
        }
    }

    @Test
    fun testProcessUnsyncedGross() = runTest {
        val job = createJob(this)
        val gross = listOf(dummyGross("Mei 2026"))

        whenever(grossRepository.getUnsyncedGross()).thenReturn(gross)
        whenever(syncService.syncGrossBatch(spreadsheetId, gross)).thenReturn(1)

        assertEquals(1, job.processUnsyncedGross())
        verify(grossRepository).markAsSynced(listOf("Mei 2026"))

        // Empty branch
        whenever(grossRepository.getUnsyncedGross()).thenReturn(emptyList())
        assertEquals(0, job.processUnsyncedGross())
    }

    @Test
    fun testProcessUnsyncedSummaries() = runTest {
        val job = createJob(this)
        val summaries = listOf(
            SpreadsheetData(key = "key1", value = "val1")
        )

        whenever(summaryRepository.getUnsyncedSummaries()).thenReturn(summaries)
        whenever(syncService.syncSummariesBatch(spreadsheetId, summaries)).thenReturn(1)

        assertEquals(1, job.processUnsyncedSummaries())
        verify(summaryRepository).markAsSynced(listOf("key1"))

        // Empty branch
        whenever(summaryRepository.getUnsyncedSummaries()).thenReturn(emptyList())
        assertEquals(0, job.processUnsyncedSummaries())
    }

    @Test
    fun testProcessAllMethods() = runTest {
        val job = createJob(this)

        val orders = listOf(dummyOrder("1"))
        val outcomes = listOf(dummyOutcome("2"))
        val packages = listOf(dummyPackage("3"))
        val gross = listOf(dummyGross("4"))
        val summaries = listOf(SpreadsheetData(key = "5", value = "v"))

        whenever(orderRepository.getAll(1, 100_000)).thenReturn(orders)
        whenever(outcomeRepository.getAll(1, 100_000)).thenReturn(outcomes)
        whenever(packageRepository.getAll()).thenReturn(packages)
        whenever(grossRepository.getAll(1, 100_000)).thenReturn(gross)
        whenever(summaryRepository.getAll()).thenReturn(summaries)

        whenever(syncService.syncAndVerifyOrdersBatch(spreadsheetId, orders)).thenReturn(listOf("1"))
        whenever(syncService.syncAndVerifyOutcomesBatch(spreadsheetId, outcomes)).thenReturn(listOf("2"))
        whenever(syncService.syncAndVerifyPackagesBatch(spreadsheetId, packages)).thenReturn(listOf("3"))
        whenever(syncService.syncGrossBatch(spreadsheetId, gross)).thenReturn(1)
        whenever(syncService.syncSummariesBatch(spreadsheetId, summaries)).thenReturn(1)

        assertEquals(1, job.processAllOrdersToSheets())
        assertEquals(1, job.processAllOutcomesToSheets())
        assertEquals(1, job.processAllPackagesToSheets())
        assertEquals(1, job.processAllGrossToSheets())
        assertEquals(1, job.processAllSummariesToSheets())
    }

    @Test
    fun testProcessPendingDeletes() = runTest {
        val job = createJob(this)
        val deletes = listOf(
            SyncDeleteEvent(id = 1, entityId = "100", entityType = SyncEntityType.ORDER)
        )

        whenever(syncDeleteEventRepository.getPending()).thenReturn(deletes)
        whenever(syncService.clearDeletedRows(spreadsheetId, deletes)).thenReturn(listOf(1))

        assertEquals(1, job.processPendingDeletes())
        verify(syncDeleteEventRepository).markProcessed(listOf(1))

        // Empty branch
        whenever(syncDeleteEventRepository.getPending()).thenReturn(emptyList())
        assertEquals(0, job.processPendingDeletes())
    }

    @Test
    fun testBackgroundSyncLoop() = runTest {
        try {
            val job = createJob(this)
            
            whenever(syncDeleteEventRepository.getPending()).thenReturn(emptyList())
            whenever(orderRepository.getUnsyncedOrders()).thenReturn(emptyList())
            whenever(outcomeRepository.getUnsyncedOutcomes()).thenReturn(emptyList())
            whenever(packageRepository.getUnsyncedPackages()).thenReturn(emptyList())

            // Start job loop
            job.start()
            runCurrent()

            // Loop runs immediately. Now advance time.
            // The default interval is 15 minutes.
            advanceTimeBy(15 * 60 * 1000L)
            runCurrent()

            // Verify state is clean and no errors recorded
            assertEquals("SUCCESS", syncStateManager.lastSyncStatus)
            assertEquals(0, syncStateManager.lastChangesCount)
        } finally {
            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testBackgroundSyncLoopError() = runTest {
        try {
            val job = createJob(this)
            
            whenever(syncDeleteEventRepository.getPending()).doThrow(RuntimeException("Sync engine down"))

            job.start()
            runCurrent()

            assertEquals("FAILED", syncStateManager.lastSyncStatus)
            assertEquals("Sync engine down", syncStateManager.lastSyncError)
        } finally {
            coroutineContext.cancelChildren()
        }
    }
}
