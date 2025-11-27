package com.raylabs.laundryhub.ui.order

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.TextUtil.removeRupiahFormat
import com.raylabs.laundryhub.ui.common.util.WhatsAppHelper
import com.raylabs.laundryhub.ui.component.DatePickerField
import com.raylabs.laundryhub.ui.component.DropdownMenuField
import com.raylabs.laundryhub.ui.order.state.OrderUiState
import com.raylabs.laundryhub.ui.order.state.isSubmitEnabled
import com.raylabs.laundryhub.ui.order.state.isUpdateEnabled
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem

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
    LaunchedEffect(key1 = state.paymentMethod, key2 = state.paymentOption) {
        if (state.paymentMethod.isBlank() && state.paymentOption.isNotEmpty()) {
            onPaymentMethodSelected(state.paymentOption.first())
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(unbounded = true)
            .heightIn(max = 900.dp)
            .background(
                MaterialTheme.colors.surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp)
    ) {
        // Drag Handle
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

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

        val orderDate = state.orderDate.ifBlank { "" }

        DatePickerField(
            label = "Order Date",
            value = orderDate,
            onDateSelected = onOrderDateSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        PackageDropdownMenuField(
            label = "Package",
            selected = state.selectedPackage,
            options = state.packageNameList.data.orEmpty(),
            onSelected = onPackageSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Minimum Order ${state.selectedPackage?.displayPrice.orEmpty()}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            style = MaterialTheme.typography.caption
        )

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
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Text("Rp", modifier = Modifier.padding(start = 4.dp))
            },
            trailingIcon = {
                Text(",-", modifier = Modifier.padding(end = 4.dp))
            },
            singleLine = true
        )

        Text(
            text = "The weight is ${state.weight.ifBlank { "0" }} Kg",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            style = MaterialTheme.typography.caption
        )

        DropdownMenuField(
            label = "Payment Method",
            value = state.paymentMethod,
            options = state.paymentOption,
            onOptionSelected = onPaymentMethodSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

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

            Button(
                onClick = if (state.isEditMode) onUpdate else onSubmit,
                enabled = if (state.isEditMode)
                    state.isUpdateEnabled && !state.isSubmitting
                else
                    state.isSubmitEnabled && !state.isSubmitting,
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .height(56.dp)
                    .defaultMinSize(minWidth = 120.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(if (state.isEditMode) "Update" else "Submit")
                }
            }
        }
    }
}

@Composable
fun PackageDropdownMenuField(
    label: String,
    selected: PackageItem?,
    options: List<PackageItem>,
    onSelected: (PackageItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = true }
    ) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            enabled = false,
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { item ->
                DropdownMenuItem(onClick = {
                    onSelected(item)
                    expanded = false
                }) {
                    Text(text = "${item.name} (${item.displayPrice})")
                }
            }
        }
    }
}


@Preview
@Composable
fun PreviewOrderBottomSheet() {
    val state = OrderUiState(
        name = "Uray Febri",
        phone = "+62 896937216252",
        selectedPackage = PackageItem("Regular", "Rp 5.000", "3d"),
        price = "Rp 25.000",
        paymentMethod = "Paid by Cash",
        note = "-",
        packageNameList = SectionState(data = listOf(PackageItem("Regular", "Rp 5.000,-", "3d"))),
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
