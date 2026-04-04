package com.raylabs.laundryhub.core.domain.model.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadsheetConfigTest {

    @Test
    fun `hasConfiguredSpreadsheet is true only when spreadsheet id exists`() {
        assertTrue(SpreadsheetConfig(spreadsheetId = "sheet-123").hasConfiguredSpreadsheet)
        assertFalse(SpreadsheetConfig().hasConfiguredSpreadsheet)
    }

    @Test
    fun `hasCurrentValidation reflects current validation version`() {
        assertTrue(
            SpreadsheetConfig(
                validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
            ).hasCurrentValidation
        )
        assertFalse(
            SpreadsheetConfig(
                validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION - 1
            ).hasCurrentValidation
        )
    }
}
