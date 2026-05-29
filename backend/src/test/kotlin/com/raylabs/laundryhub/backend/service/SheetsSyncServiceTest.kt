package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.shared.network.model.sheets.AppendValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.UpdateValuesResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class SheetsSyncServiceTest {

    @Test
    fun `acknowledgedUpdateKeys returns keys with updated rows or cells`() {
        val acknowledged = acknowledgedUpdateKeys(
            keys = listOf("1674", "1675", "1676"),
            response = BatchUpdateValuesResponse(
                responses = listOf(
                    UpdateValuesResponse(updatedRows = 1, updatedCells = 12),
                    UpdateValuesResponse(updatedRows = 0, updatedCells = 0),
                    UpdateValuesResponse(updatedRows = null, updatedCells = 12)
                )
            )
        )

        assertEquals(listOf("1674", "1676"), acknowledged)
    }

    @Test
    fun `acknowledgedUpdateKeys returns empty when Sheets reports no updated rows or cells`() {
        val acknowledged = acknowledgedUpdateKeys(
            keys = listOf("1674"),
            response = BatchUpdateValuesResponse(
                responses = listOf(UpdateValuesResponse(updatedRows = 0, updatedCells = 0))
            )
        )

        assertEquals(emptyList(), acknowledged)
    }

    @Test
    fun `acknowledgedAppendKeys returns all keys when append covers every row`() {
        val acknowledged = acknowledgedAppendKeys(
            keys = listOf("1674", "1675"),
            response = AppendValuesResponse(
                updates = UpdateValuesResponse(updatedRows = 2, updatedCells = 24)
            )
        )

        assertEquals(listOf("1674", "1675"), acknowledged)
    }

    @Test
    fun `acknowledgedAppendKeys returns empty when append response does not cover every row`() {
        val acknowledged = acknowledgedAppendKeys(
            keys = listOf("1674", "1675"),
            response = AppendValuesResponse(
                updates = UpdateValuesResponse(updatedRows = 1, updatedCells = 12)
            )
        )

        assertEquals(emptyList(), acknowledged)
    }
}
