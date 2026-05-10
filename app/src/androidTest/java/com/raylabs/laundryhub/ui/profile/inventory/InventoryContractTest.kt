package com.raylabs.laundryhub.ui.profile.inventory

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.raylabs.laundryhub.ui.common.dummy.inventory.dummyInventoryUiState
import com.raylabs.laundryhub.ui.component.InventoryPackageEditorSheet
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InventoryContractTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun inventoryContent_keepsPackageMasterAndSuggestionContract() {
        var addClicks = 0
        var selectedPackage = ""
        var selectedSuggestion = ""

        composeRule.setContent {
            LaundryHubTheme {
                val bannerState = rememberInlineAdaptiveBannerAdState("inventory_contract")
                InventoryContent(
                    state = dummyInventoryUiState,
                    bannerState = bannerState,
                    modifier = Modifier,
                    onAddPackage = { addClicks++ },
                    onPackageClick = { selectedPackage = it.name },
                    onSuggestedPackageClick = { selectedSuggestion = it }
                )
            }
        }

        composeRule.onNodeWithText("Package master").assertIsDisplayed()
        composeRule.onNodeWithText("Add package").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, addClicks) }

        composeRule.onNodeWithText("Regular").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals("Regular", selectedPackage) }

        composeRule.onNodeWithText("Unregistered package names").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Express Kilat").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals("Express Kilat", selectedSuggestion) }
    }

    @Test
    fun packageEditor_disablesSaveWhenRequiredFieldsAreMissing() {
        composeRule.setContent {
            LaundryHubTheme {
                InventoryPackageEditorSheet(
                    visible = true,
                    isEditMode = false,
                    packageName = "",
                    packagePrice = "",
                    packageDuration = "",
                    packageUnit = "",
                    isSaveEnabled = false,
                    isSubmitting = false,
                    onPackageNameChange = {},
                    onPackagePriceChange = {},
                    onPackageDurationChange = {},
                    onPackageUnitChange = {},
                    onDismiss = {},
                    onSave = {}
                )
            }
        }

        composeRule.onAllNodesWithText("Add package")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Package name").assertIsDisplayed()
        composeRule.onNodeWithText("Price per unit").assertIsDisplayed()
        composeRule.onNodeWithText("Duration").assertIsDisplayed()
        composeRule.onNodeWithText("Unit").assertIsDisplayed()
        composeRule.onAllNodesWithText("Add package")[1].assertIsNotEnabled()
    }

    @Test
    fun packageEditor_callsSaveWhenValid() {
        var saveClicks = 0

        composeRule.setContent {
            LaundryHubTheme {
                InventoryPackageEditorSheet(
                    visible = true,
                    isEditMode = false,
                    packageName = "Express Test",
                    packagePrice = "10000",
                    packageDuration = "1d",
                    packageUnit = "kg",
                    isSaveEnabled = true,
                    isSubmitting = false,
                    onPackageNameChange = {},
                    onPackagePriceChange = {},
                    onPackageDurationChange = {},
                    onPackageUnitChange = {},
                    onDismiss = {},
                    onSave = { saveClicks++ }
                )
            }
        }

        composeRule.onAllNodesWithText("Add package")[1].assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, saveClicks) }
    }
}
