package com.raylabs.laundryhub.ui.sync

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import org.junit.Rule
import org.junit.Test

class SyncSettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun displaysSyncSettingsCorrectly() {
        val dummyState = SyncSettingsUiState(
            lastSyncTime = "2026-05-11T12:00:00",
            changesCount = 10,
            autoSyncIntervalMinutes = 30,
            reverseSyncSchedule = ReverseSyncSchedule.TWICE_DAILY
        )

        composeRule.setContent {
            LaundryHubTheme {
                SyncSettingsScreenContent(
                    state = dummyState,
                    onNavigateBack = {},
                    onMasterSourceSelected = {},
                    onCheckDifferencesClick = {},
                    onConfirmSyncNow = {},
                    onDismissPreview = {},
                    onClearMessages = {}
                )
            }
        }

        // Verify status card elements
        composeRule.onNodeWithText("Sync Status").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("10 items").performScrollTo().assertIsDisplayed()

        // Verify sections exist
        composeRule.onNodeWithText("Master Data Source").performScrollTo().assertIsDisplayed()
        
        // Verify options
        composeRule.onNodeWithText("Google Sheets").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("App Database").performScrollTo().assertIsDisplayed()

        // Verify primary button
        composeRule.onNodeWithText("Check differences").assertIsDisplayed()
    }
}
