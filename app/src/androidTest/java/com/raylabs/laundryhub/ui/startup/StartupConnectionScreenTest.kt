package com.raylabs.laundryhub.ui.startup

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartupConnectionScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsCheckingState() {
        var clicked = false
        composeRule.setContent {
            LaundryHubTheme {
                StartupConnectionScreen(
                    uiState = StartupConnectionUiState.Checking,
                    onCheckAgain = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_checking_title)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_checking_message)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_checking_caption)).assertIsDisplayed()
    }

    @Test
    fun showsMaintenanceState_withCustomMessage() {
        var clicked = false
        val customMessage = "Custom Maintenance Message"
        
        composeRule.setContent {
            LaundryHubTheme {
                StartupConnectionScreen(
                    uiState = StartupConnectionUiState.Maintenance(customMessage),
                    onCheckAgain = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_unavailable_title)).assertIsDisplayed()
        composeRule.onNodeWithText(customMessage).assertIsDisplayed()
        
        val button = composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_check_again))
        button.assertIsDisplayed()
        button.assertIsEnabled()
        button.performClick()
        
        assertTrue(clicked)
    }

    @Test
    fun showsUnavailableState_withDefaultMessage() {
        var clicked = false
        
        composeRule.setContent {
            LaundryHubTheme {
                StartupConnectionScreen(
                    uiState = StartupConnectionUiState.Unavailable,
                    onCheckAgain = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_unavailable_title)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_unavailable_message)).assertIsDisplayed()
        
        val button = composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_check_again))
        button.assertIsDisplayed()
        button.assertIsEnabled()
        button.performClick()
        
        assertTrue(clicked)
    }

    @Test
    fun showsRetryingState_withDisabledButton() {
        composeRule.setContent {
            LaundryHubTheme {
                StartupConnectionScreen(
                    uiState = StartupConnectionUiState.Retrying,
                    onCheckAgain = { }
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_unavailable_title)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_unavailable_message)).assertIsDisplayed()
        
        val button = composeRule.onNodeWithText(composeRule.activity.getString(R.string.startup_connection_checking_button))
        button.assertIsDisplayed()
        button.assertIsNotEnabled()
    }
}
