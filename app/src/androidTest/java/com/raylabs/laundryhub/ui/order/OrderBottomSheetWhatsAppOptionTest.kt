package com.raylabs.laundryhub.ui.order

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.order.state.OrderUiState
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import org.junit.Rule
import org.junit.Test

class OrderBottomSheetWhatsAppOptionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsWhatsAppField_whenEnabled_newOrder() {
        setContentWithState(showWhatsAppOption = true, isEditMode = false)
        composeRule.onNodeWithText(phoneNumberLabel()).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(whatsAppHint()).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hidesWhatsAppField_whenDisabled_newOrder() {
        setContentWithState(showWhatsAppOption = false, isEditMode = false)
        composeRule.onAllNodesWithText(phoneNumberLabel()).assertCountEquals(0)
        composeRule.onAllNodesWithText(whatsAppHint()).assertCountEquals(0)
    }

    @Test
    fun showsWhatsAppField_whenEnabled_editOrder() {
        setContentWithState(showWhatsAppOption = true, isEditMode = true)
        composeRule.onNodeWithText(phoneNumberLabel()).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(whatsAppHint()).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hidesWhatsAppField_whenDisabled_editOrder() {
        setContentWithState(showWhatsAppOption = false, isEditMode = true)
        composeRule.onAllNodesWithText(phoneNumberLabel()).assertCountEquals(0)
        composeRule.onAllNodesWithText(whatsAppHint()).assertCountEquals(0)
    }

    private fun phoneNumberLabel(): String = composeRule.activity.getString(R.string.phone_number)

    private fun whatsAppHint(): String = composeRule.activity.getString(R.string.whatsapp_available_hint)

    private fun setContentWithState(showWhatsAppOption: Boolean, isEditMode: Boolean) {
        composeRule.setContent {
            LaundryHubTheme {
                OrderBottomSheet(
                    state = OrderUiState(
                        name = "Test User",
                        phone = "8123",
                        selectedPackage = PackageItem("Regular", "Rp 5.000", "3d"),
                        price = "Rp 25.000",
                        paymentMethod = "Paid by Cash",
                        note = "-",
                        packageNameList = SectionState(
                            data = listOf(PackageItem("Regular", "Rp 5.000,-", "3d"))
                        ),
                        paymentOption = listOf("Paid by Cash", "Paid by QRIS"),
                        showWhatsAppOption = showWhatsAppOption,
                        isEditMode = isEditMode
                    ),
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
    }
}
