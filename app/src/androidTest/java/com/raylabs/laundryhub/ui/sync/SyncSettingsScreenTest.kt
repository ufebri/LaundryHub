package com.raylabs.laundryhub.ui.sync

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
                    onIntervalSelected = {},
                    onScheduleSelected = {},
                    onSyncNowClick = {},
                    onClearMessages = {}
                )
            }
        }

        // Verify status card elements
        composeRule.onNodeWithText("Status Sinkronisasi").assertIsDisplayed()
        composeRule.onNodeWithText("10 item").assertIsDisplayed()

        // Verify sections exist
        composeRule.onNodeWithText("Interval Sinkronisasi Otomatis").assertIsDisplayed()
        composeRule.onNodeWithText("Jadwal Tarik Data (Sheets -> App)").assertIsDisplayed()
        
        // Verify options
        composeRule.onNodeWithText("30 Menit").assertIsDisplayed()
        composeRule.onNodeWithText("12:00 & 23:00 WIB").assertIsDisplayed()

        // Verify primary button
        composeRule.onNodeWithText("Sinkronisasi Sekarang").assertIsDisplayed()
    }
}

