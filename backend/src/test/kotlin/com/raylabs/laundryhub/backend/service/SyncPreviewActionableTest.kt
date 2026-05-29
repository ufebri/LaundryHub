package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.*
import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncPreviewActionableTest {

    private val orderRepository: OrderRepository = mock()
    private val outcomeRepository: OutcomeRepository = mock()
    private val packageRepository: PackageRepository = mock()
    private val grossRepository: GrossRepository = mock()
    private val summaryRepository: SummaryRepository = mock()
    private val syncDeleteEventRepository: SyncDeleteEventRepository = mock()
    private val syncService: SheetsSyncService = mock()
    private val spreadsheetId = "test-sheet-id"

    private val service = SyncPreviewService(
        orderRepository,
        outcomeRepository,
        packageRepository,
        grossRepository,
        summaryRepository,
        syncDeleteEventRepository,
        syncService,
        spreadsheetId
    )

    @Test
    fun `SUPABASE source of truth includes Gross and Summary in totalDifferences when sync is enabled`() = runTest {
        // Arrange
        whenever(syncDeleteEventRepository.getPending()).doReturn(emptyList())
        
        // Orders: 1 difference (changedRows)
        whenever(syncService.fetchOrdersFromSheet(any())).doReturn(listOf(testOrder("1", "Paid")))
        whenever(orderRepository.getAll(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).doReturn(listOf(testOrder("1", "Unpaid")))
        
        // Gross: 1 difference (onlyInSheets)
        whenever(syncService.fetchGrossFromSheet(any())).doReturn(listOf(GrossData(month = "Jan 2026", totalNominal = "100", orderCount = "1", tax = "0")))
        whenever(grossRepository.getAll(any(), any())).doReturn(emptyList())
        
        // Summary: 1 difference (onlyInSheets)
        whenever(syncService.fetchSummaryFromSheet(any())).doReturn(listOf(SpreadsheetData("key1", "val1")))
        whenever(summaryRepository.getAll()).doReturn(emptyList())

        whenever(syncService.fetchOutcomesFromSheet(any())).doReturn(emptyList())
        whenever(outcomeRepository.getAll(any(), any())).doReturn(emptyList())
        whenever(packageRepository.getAll()).doReturn(emptyList())
        whenever(syncService.fetchPackagesFromSheet(any())).doReturn(emptyList())

        // Act
        val preview = service.createPreview(MasterSourceOfTruth.SUPABASE)

        // Assert
        // Orders (1) + Gross (1) + Summary (1) = 3
        assertEquals(3, preview.totalDifferences)
        
        // Ensure Gross and Summary still have differences in their individual entity previews
        val grossEntity = preview.entities.find { it.entity == "Gross" }!!
        assertEquals(1, grossEntity.totalDifferences)
        
        val summaryEntity = preview.entities.find { it.entity == "Summary" }!!
        assertEquals(1, summaryEntity.totalDifferences)
    }

    @Test
    fun `SHEETS source of truth includes Gross and Summary in totalDifferences`() = runTest {
        // Arrange
        whenever(syncDeleteEventRepository.getPending()).doReturn(emptyList())
        
        // Orders: 1 difference
        whenever(syncService.fetchOrdersFromSheet(any())).doReturn(listOf(testOrder("1", "Paid")))
        whenever(orderRepository.getAll(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).doReturn(listOf(testOrder("1", "Unpaid")))
        
        // Gross: 1 difference
        whenever(syncService.fetchGrossFromSheet(any())).doReturn(listOf(GrossData(month = "Jan 2026", totalNominal = "100", orderCount = "1", tax = "0")))
        whenever(grossRepository.getAll(any(), any())).doReturn(emptyList())
        
        // Summary: 1 difference
        whenever(syncService.fetchSummaryFromSheet(any())).doReturn(listOf(SpreadsheetData("key1", "val1")))
        whenever(summaryRepository.getAll()).doReturn(emptyList())

        whenever(syncService.fetchOutcomesFromSheet(any())).doReturn(emptyList())
        whenever(outcomeRepository.getAll(any(), any())).doReturn(emptyList())
        whenever(packageRepository.getAll()).doReturn(emptyList())
        whenever(syncService.fetchPackagesFromSheet(any())).doReturn(emptyList())

        // Act
        val preview = service.createPreview(MasterSourceOfTruth.SHEETS)

        // Assert
        // Orders (1) + Gross (1) + Summary (1) = 3
        assertEquals(3, preview.totalDifferences)
    }

    private fun testOrder(id: String, paidStatus: String) = OrderData(
        orderId = id,
        orderDate = "2026-05-29",
        name = "Test",
        weight = "1",
        priceKg = "1000",
        totalPrice = "1000",
        paidStatus = paidStatus,
        packageName = "Test",
        remark = "",
        paymentMethod = "Cash",
        phoneNumber = "",
        dueDate = ""
    )
}
