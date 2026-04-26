package com.raylabs.laundryhub.ui.order

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.TextUtil.removeRupiahFormat
import com.raylabs.laundryhub.ui.common.util.WhatsAppHelper
import com.raylabs.laundryhub.ui.component.DatePickerField
import com.raylabs.laundryhub.ui.component.HorizontalSelectionCards
import com.raylabs.laundryhub.ui.component.SingleSelectChipRow
import com.raylabs.laundryhub.ui.component.SubmitUpdateButton
import com.raylabs.laundryhub.ui.order.state.OrderUiState
import com.raylabs.laundryhub.ui.order.state.isSubmitEnabled
import com.raylabs.laundryhub.ui.order.state.isUpdateEnabled
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import com.raylabs.laundryhub.ui.theme.modalSheetTop

private const val ORDER_SHEET_DESCRIPTION = "Order sheet"
private const val ORDER_NAME_FIELD_DESCRIPTION = "Order name field"
private const val ORDER_PRICE_FIELD_DESCRIPTION = "Order price field"
private const val ORDER_SUBMIT_BUTTON_DESCRIPTION = "Submit order"
private const val ORDER_UPDATE_BUTTON_DESCRIPTION = "Update order"

@Composable
fun OrderBottomSheet(
    state: OrderUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onPriceChanged: (String) -> Unit,
    onPackageSelected: (PackageItem) -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onOrderDateSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orderDate = state.orderDate.ifBlank { "" }

    LaunchedEffect(key1 = state.paymentMethod, key2 = state.paymentOption) {
        if (state.paymentMethod.isBlank() && state.paymentOption.isNotEmpty()) {
            onPaymentMethodSelected(state.paymentOption.first())
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(unbounded = true)
            .heightIn(max = 900.dp)
            .semantics {
                contentDescription = ORDER_SHEET_DESCRIPTION
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(unbounded = true)
                .heightIn(max = 900.dp)
                .verticalScroll(rememberScrollState())
                .background(
                    MaterialTheme.colors.surface,
                    shape = MaterialTheme.shapes.modalSheetTop
                )
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color.Gray, shape = RoundedCornerShape(2.dp))
            )

            Text(
                text = if (state.isEditMode) "Update Order" else "New Order",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = { if (it.length <= 30) onNameChanged(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                label = { Text("Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = ORDER_NAME_FIELD_DESCRIPTION
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.showWhatsAppOption) {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { if (it.length <= 13) onPhoneChanged(it) },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Number
                    ),
                    leadingIcon = {
                        Text(
                            text = "+62",
                            style = MaterialTheme.typography.body1
                        )
                    },
                    trailingIcon = {
                        if (state.isEditMode && state.phone.length > 9) {
                            val context = LocalContext.current
                            val message = WhatsAppHelper.buildOrderMessage(
                                customerName = state.name,
                                packageName = state.selectedPackage?.name.orEmpty(),
                                total = state.price,
                                paymentStatus = state.paymentMethod
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.clickable {
                                    WhatsAppHelper.sendWhatsApp(context, state.phone, message)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                LaunchedEffect(state.isEditMode, state.phone) {
                    Log.d(
                        "OrderBottomSheet",
                        "PhoneField Rendered: isEditMode=${state.isEditMode}, phone=${state.phone}"
                    )
                }

                Text(
                    text = "Make sure the WhatsApp is Available",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }

            DatePickerField(
                label = "Order Date",
                value = orderDate,
                onDateSelected = onOrderDateSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalSelectionCards(
                label = "Package",
                options = state.packageNameList.data.orEmpty(),
                selectedOption = state.selectedPackage,
                onOptionSelected = onPackageSelected,
                optionTitle = { it.name },
                optionSupportingText = { packageRateText(it) },
                optionTrailingText = { packageDurationText(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.price.removeRupiahFormat(),
                onValueChange = { input ->
                    val rawDigits = input.filter { it.isDigit() }.take(7)
                    onPriceChanged(rawDigits)
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                label = { Text("Price") },
                leadingIcon = {
                    Text("Rp", modifier = Modifier.padding(start = 4.dp))
                },
                trailingIcon = {
                    Text(",-", modifier = Modifier.padding(end = 4.dp))
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = ORDER_PRICE_FIELD_DESCRIPTION
                    }
            )

            Text(
                text = "The weight is ${state.weight.ifBlank { "0" }} Kg",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                style = MaterialTheme.typography.caption
            )

            Spacer(modifier = Modifier.height(12.dp))

            SingleSelectChipRow(
                label = "Payment Method",
                options = state.paymentOption,
                selectedValue = state.paymentMethod,
                onOptionSelected = onPaymentMethodSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { if (it.length <= 30) onNoteChanged(it) },
                    label = { Text("Note") },
                    modifier = Modifier.weight(1f)
                )

                SubmitUpdateButton(
                    isEditMode = state.isEditMode,
                    isEnabled = if (state.isEditMode)
                        state.isUpdateEnabled && !state.isSubmitting
                    else
                        state.isSubmitEnabled && !state.isSubmitting,
                    isSubmitting = state.isSubmitting,
                    onSubmit = onSubmit,
                    onUpdate = onUpdate,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .semantics {
                            contentDescription = if (state.isEditMode) {
                                ORDER_UPDATE_BUTTON_DESCRIPTION
                            } else {
                                ORDER_SUBMIT_BUTTON_DESCRIPTION
                            }
                        },
                    fillMaxWidth = false
                )
            }
        }
    }
}

private fun packageDurationText(item: PackageItem): String {
    val duration = item.work.trim()
    return when {
        duration.equals("same day", ignoreCase = true) -> "Same day"
        duration.endsWith("h", ignoreCase = true) -> {
            val totalHours = duration.dropLast(1).toIntOrNull()
            if (totalHours != null) {
                "$totalHours Hours"
            } else {
                duration
            }
        }

        duration.endsWith("d", ignoreCase = true) -> {
            val totalDays = duration.dropLast(1).toIntOrNull()
            if (totalDays != null) {
                "$totalDays Days"
            } else {
                duration
            }
        }

        duration.isNotBlank() -> duration
        else -> ""
    }
}

private fun packageRateText(item: PackageItem): String {
    return item.displayRate
}

@Preview
@Composable
fun PreviewOrderBottomSheet() {
    val state = OrderUiState(
        name = "Uray Febri",
        phone = "+62 896937216252",
        selectedPackage = PackageItem("Regular", "Rp 5.000", "3d", "kg"),
        price = "Rp 25.000",
        paymentMethod = "Paid by Cash",
        note = "-",
        packageNameList = SectionState(
            data = listOf(
                PackageItem("Regular", "Rp 5.000", "3d", "kg"),
                PackageItem("Express - 6H", "Rp 10.000", "6h", "kg"),
                PackageItem("Express - 24H", "Rp 8.000", "1d", "kg")
            )
        ),
        paymentOption = listOf("Paid by Cash", "Paid by QRIS")
    )

    OrderBottomSheet(
        state = state,
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
