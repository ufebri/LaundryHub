package com.raylabs.laundryhub.ui.outcome

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.raylabs.laundryhub.core.domain.model.sheets.PAID_BY_CASH
import com.raylabs.laundryhub.core.domain.model.sheets.PAID_BY_PERSONAL
import com.raylabs.laundryhub.core.domain.model.sheets.PAID_BY_QRIS
import com.raylabs.laundryhub.ui.component.OutcomeBottomSheet
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OutcomeBottomSheetContractTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun newOutcome_keepsMasterFormContract() {
        setContentWithState(OutcomeUiState())

        composeRule.onNodeWithText("Add Outcome").assertIsDisplayed()
        composeRule.onNodeWithText("Date").assertIsDisplayed()
        composeRule.onNodeWithText("Purpose").assertIsDisplayed()
        composeRule.onNodeWithText("Price").assertIsDisplayed()
        composeRule.onNodeWithText("Payment Method").assertIsDisplayed()
        composeRule.onNodeWithText(PAID_BY_CASH).assertIsDisplayed()
        composeRule.onNodeWithText(PAID_BY_QRIS).assertIsDisplayed()
        composeRule.onNodeWithText(PAID_BY_PERSONAL).assertIsDisplayed()
        composeRule.onNodeWithText("Remark").assertIsDisplayed()
        composeRule.onNodeWithText("Submit").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun editOutcome_usesUpdateContract_whenRequiredFieldsArePresent() {
        var updateClicks = 0

        setContentWithState(
            state = OutcomeUiState(
                outcomeID = "42",
                name = "Detergent",
                date = "2026-05-09",
                price = "12000",
                paymentStatus = PAID_BY_CASH,
                isEditMode = true
            ),
            onUpdate = { updateClicks++ }
        )

        composeRule.onNodeWithText("Update Outcome").assertIsDisplayed()
        composeRule.onNodeWithText("Update").assertIsDisplayed().assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertEquals(1, updateClicks)
        }
    }

    private fun setContentWithState(
        state: OutcomeUiState,
        onUpdate: () -> Unit = {}
    ) {
        composeRule.setContent {
            LaundryHubTheme {
                OutcomeBottomSheet(
                    state = state,
                    onPurposeChanged = {},
                    onPriceChanged = {},
                    onPaymentMethodSelected = {},
                    onRemarkChanged = {},
                    onDateSelected = {},
                    onUpdate = onUpdate,
                    onSubmit = {}
                )
            }
        }
    }
}
