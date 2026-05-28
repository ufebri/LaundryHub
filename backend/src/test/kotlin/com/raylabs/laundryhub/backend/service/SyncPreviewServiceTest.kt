package com.raylabs.laundryhub.backend.service

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncPreviewServiceTest {

    @Test
    fun `buildEntityPreview counts missing changed duplicate and pending rows`() {
        val preview = buildEntityPreview(
            entity = "Orders",
            sheetRows = listOf(
                TestRow("1", "same"),
                TestRow("2", "sheet"),
                TestRow("2", "duplicate"),
                TestRow("3", "sheet-only")
            ),
            databaseRows = listOf(
                TestRow("1", "same"),
                TestRow("2", "database"),
                TestRow("4", "database-only")
            ),
            pendingDeletes = 2,
            keySelector = TestRow::id,
            signatureSelector = TestRow::signature
        )

        assertEquals(1, preview.onlyInSheets)
        assertEquals(1, preview.onlyInDatabase)
        assertEquals(1, preview.changedRows)
        assertEquals(1, preview.duplicateKeys)
        assertEquals(2, preview.pendingDeletes)
        assertEquals(6, preview.totalDifferences)
    }

    @Test
    fun `buildEntityPreview flags header rows as suspicious differences`() {
        val preview = buildEntityPreview(
            entity = "Orders",
            sheetRows = listOf(TestRow("1", "same")),
            databaseRows = listOf(
                TestRow("1", "same"),
                TestRow("orderID", "header")
            ),
            pendingDeletes = 0,
            keySelector = TestRow::id,
            signatureSelector = TestRow::signature,
            suspiciousKeySelector = ::isOrderHeaderKey
        )

        assertEquals(1, preview.onlyInDatabase)
        assertEquals(1, preview.suspiciousRows)
        assertEquals(1, preview.totalDifferences)
    }
}

private data class TestRow(
    val id: String,
    val signature: String
)
