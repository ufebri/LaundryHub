package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.backend.service.SheetsSyncService
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals

class GrossRouteBehaviorTest {

    private val repository: GrossRepository = mock()
    private val syncService: SheetsSyncService = mock()

    @Test
    fun `gross response uses Sheet data when available`() = runTest {
        whenever(syncService.fetchGrossFromSheet(any())).doReturn(
            listOf(GrossData(month = "Mei 2026", totalNominal = "Rp3.343.000", orderCount = "115 order", tax = "Rp16.715"))
        )

        val rows = fetchGrossForResponse(repository, syncService, spreadsheetId = "sheet-id", page = 1, size = 1)

        assertEquals("Rp3.343.000", rows.single().totalNominal)
        verify(repository, never()).getAll(any(), any())
    }

    @Test
    fun `gross response sorts Sheet rows newest first and applies paging`() = runTest {
        whenever(syncService.fetchGrossFromSheet(any())).doReturn(
            listOf(
                GrossData(month = "Maret 2025", totalNominal = "Rp1.038.150", orderCount = "35", tax = "Rp5.191"),
                GrossData(month = "April 2026", totalNominal = "Rp4.101.000", orderCount = "148", tax = "Rp20.505"),
                GrossData(month = "Mei 2026", totalNominal = "Rp3.343.000", orderCount = "115", tax = "Rp16.715")
            )
        )

        val firstPage = fetchGrossForResponse(repository, syncService, spreadsheetId = "sheet-id", page = 1, size = 2)
        val secondPage = fetchGrossForResponse(repository, syncService, spreadsheetId = "sheet-id", page = 2, size = 2)

        assertEquals(listOf("Mei 2026", "April 2026"), firstPage.map { it.month })
        assertEquals(listOf("Maret 2025"), secondPage.map { it.month })
        verify(repository, never()).getAll(any(), any())
    }

    @Test
    fun `gross response falls back to database cache when Sheet data is unavailable`() = runTest {
        whenever(syncService.fetchGrossFromSheet(any())).doReturn(emptyList())
        whenever(repository.getAll(any(), any())).doReturn(
            listOf(GrossData(month = "Mei 2026", totalNominal = "Rp3.139.000", orderCount = "108 order", tax = "Rp15.695"))
        )

        val rows = fetchGrossForResponse(repository, syncService, spreadsheetId = "sheet-id", page = 1, size = 1)

        assertEquals("Rp3.139.000", rows.single().totalNominal)
    }

    @Test
    fun `gross response falls back to database cache when spreadsheet id is missing`() = runTest {
        whenever(repository.getAll(any(), any())).doReturn(
            listOf(GrossData(month = "Mei 2026", totalNominal = "Rp3.139.000", orderCount = "108 order", tax = "Rp15.695"))
        )

        val rows = fetchGrossForResponse(repository, syncService, spreadsheetId = null, page = 1, size = 1)

        assertEquals("108 order", rows.single().orderCount)
        verify(syncService, never()).fetchGrossFromSheet(any())
    }
}
