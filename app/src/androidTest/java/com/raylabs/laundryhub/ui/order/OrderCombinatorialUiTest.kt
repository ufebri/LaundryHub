package com.raylabs.laundryhub.ui.order

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.order.state.OrderUiState
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import org.junit.Rule
import org.junit.Test

class OrderCombinatorialUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    data class TestData(
        val name: String,
        val price: String,
        val hasPackage: Boolean,
        val hasPayment: Boolean,
        val expectedEnabled: Boolean
    )

    @Test
    fun testSubmitButtonStateCombinations() {
        val testCases = listOf(
            TestData("", "", false, false, false),           // All empty
            TestData("Uray", "10000", true, true, true),    // All valid
            TestData("", "10000", true, true, false),       // Missing name
            TestData("Uray", "", true, true, false),         // Missing price
            TestData("Uray", "10000", false, true, false),  // Missing package
            TestData("Uray", "10000", true, false, false)   // Missing payment
        )

        val stateFlow = mutableStateOf(OrderUiState())

        composeRule.setContent {
            LaundryHubTheme {
                OrderBottomSheet(
                    state = stateFlow.value,
                    onNameChanged = {},
                    onPriceChanged = {},
                    onPhoneChanged = {},
                    onPackageSelected = {},
                    onPaymentMethodSelected = {},
                    onNoteChanged = {},
                    onOrderDateSelected = {},
                    onSubmit = {},
                    onUpdate = {}
                )
            }
        }

        testCases.forEach { case ->
            val packageItem = if (case.hasPackage) PackageItem("Reg", "5000", "3d") else null
            val paymentMethod = if (case.hasPayment) "Cash" else ""

            // Update state and trigger recomposition
            stateFlow.value = OrderUiState(
                name = case.name,
                price = case.price,
                selectedPackage = packageItem,
                paymentMethod = paymentMethod,
                packageNameList = SectionState(data = listOfNotNull(packageItem)),
                paymentOption = listOf("Cash", "QRIS")
            )

            composeRule.waitForIdle()

            val submitButton = composeRule.onNodeWithContentDescription("Submit order")
            if (case.expectedEnabled) {
                submitButton.assertIsEnabled()
            } else {
                submitButton.assertIsNotEnabled()
            }
        }
    }
}
