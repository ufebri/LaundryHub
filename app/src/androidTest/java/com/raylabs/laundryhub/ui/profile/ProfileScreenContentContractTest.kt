package com.raylabs.laundryhub.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.raylabs.laundryhub.ui.common.dummy.profile.dummyProfileUiState
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileScreenContentContractTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun profileContent_keepsMasterSectionAndNavigationContract() {
        var inventoryClicks = 0
        var reminderClicks = 0

        setContentWithState(
            onInventoryClick = { inventoryClicks++ },
            onReminderSettingsClick = { reminderClicks++ }
        )

        composeRule.onNodeWithText("Ray Febri").assertIsDisplayed()
        composeRule.onNodeWithText("rayfebri@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Store").assertIsDisplayed()
        composeRule.onNodeWithText("Inventory").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(1, inventoryClicks)
        }

        composeRule.onNodeWithText("Settings").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reminder & Cross-check")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, reminderClicks)
        }

        composeRule.onNodeWithText("WhatsApp Option").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Clear Cache").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Account").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sign Out").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun clearCache_opensAndDismissesConfirmationContract() {
        var state by mutableStateOf(dummyProfileUiState)

        setContentWithState(
            stateProvider = { state },
            onClearCacheClick = { state = state.copy(showClearCacheDialog = true) },
            onDismissClearCache = { state = state.copy(showClearCacheDialog = false) }
        )

        composeRule.onNodeWithText("Clear Cache").performScrollTo().performClick()
        composeRule.onNodeWithText("App cache will be removed. Continue?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText("App cache will be removed. Continue?").assertCountEquals(0)
    }

    private fun setContentWithState(
        stateProvider: () -> ProfileUiState = { dummyProfileUiState },
        onInventoryClick: () -> Unit = {},
        onReminderSettingsClick: () -> Unit = {},
        onClearCacheClick: () -> Unit = {},
        onDismissClearCache: () -> Unit = {}
    ) {
        composeRule.setContent {
            LaundryHubTheme {
                val bannerState = rememberInlineAdaptiveBannerAdState("profile_contract")
                ProfileScreenContent(
                    state = stateProvider(),
                    bannerState = bannerState,
                    onLoggedOut = {},
                    onInventoryClick = onInventoryClick,
                    onReminderSettingsClick = onReminderSettingsClick,
                    onWhatsAppOptionChanged = {},
                    onClearCacheClick = onClearCacheClick,
                    onConfirmClearCache = {},
                    onDismissClearCache = onDismissClearCache
                )
            }
        }
    }
}
