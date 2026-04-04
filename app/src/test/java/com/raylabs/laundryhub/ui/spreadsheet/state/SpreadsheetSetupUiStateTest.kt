package com.raylabs.laundryhub.ui.spreadsheet.state

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadsheetSetupUiStateTest {

    @Test
    fun `isBusy is true while restoring or validating`() {
        assertTrue(SpreadsheetSetupUiState(isRestoring = true).isBusy)
        assertTrue(SpreadsheetSetupUiState(isValidating = true).isBusy)
        assertFalse(SpreadsheetSetupUiState().isBusy)
    }

    @Test
    fun `hasCurrentConfiguredSpreadsheetValidation follows config validation version`() {
        assertTrue(
            SpreadsheetSetupUiState(
                configuredValidationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
            ).hasCurrentConfiguredSpreadsheetValidation
        )
        assertFalse(
            SpreadsheetSetupUiState(
                configuredValidationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION - 1
            ).hasCurrentConfiguredSpreadsheetValidation
        )
    }
}
