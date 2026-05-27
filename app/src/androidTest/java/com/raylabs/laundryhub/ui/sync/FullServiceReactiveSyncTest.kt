package com.raylabs.laundryhub.ui.sync

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raylabs.laundryhub.ui.home.InfoCardSection
import com.raylabs.laundryhub.ui.home.state.SummaryItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullServiceReactiveSyncTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifySummaryCardsShowLoadingSpinnerAndPersistDataDuringSync() {
        val mockSummaryData = listOf(
            SummaryItem("Gross Income", "Rp 100.000", "2 orders", Color.Green, Color.Black, true),
            SummaryItem("Ready to Pick", "3", "Items", Color.Blue, Color.White, false)
        )

        composeTestRule.setContent {
            InfoCardSection(
                summary = mockSummaryData,
                isRefreshing = true, // Simulate active polling sync
                onGrossCardClick = {}
            )
        }

        // 1. Verify "Keep-Last-Value" works: Old data remains visible while refreshing
        composeTestRule.onNodeWithText("Rp 100.000").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        
        // 2. Verify UI Feedback: Loading spinner appears in the specific cards
        composeTestRule.onNodeWithTag("sync_spinner_Gross Income").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sync_spinner_Ready to Pick").assertIsDisplayed()
    }

    @Test
    fun verifySummaryCardsHideLoadingSpinnerWhenSyncCompletes() {
        val mockSummaryData = listOf(
            SummaryItem("Gross Income", "Rp 150.000", "3 orders", Color.Green, Color.Black, true)
        )

        composeTestRule.setContent {
            InfoCardSection(
                summary = mockSummaryData,
                isRefreshing = false, // Simulate sync finished
                onGrossCardClick = {}
            )
        }

        // 1. Verify new data is displayed
        composeTestRule.onNodeWithText("Rp 150.000").assertIsDisplayed()
        
        // 2. Verify UI Feedback: Loading spinner disappears
        composeTestRule.onNodeWithTag("sync_spinner_Gross Income").assertDoesNotExist()
    }
}
