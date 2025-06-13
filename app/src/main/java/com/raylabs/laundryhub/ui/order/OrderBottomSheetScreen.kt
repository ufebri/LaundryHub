package com.raylabs.laundryhub.ui.order

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
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.inventory.state.PackageItem
import com.raylabs.laundryhub.ui.order.state.OrderUiState

@Composable
fun OrderBottomSheet(
    state: OrderUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onPackageSelected: (PackageItem) -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(unbounded = true)
            .heightIn(max = 500.dp)
            .padding(24.dp)
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
            text = "New Order",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChanged,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChanged,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Make sure the WhatsApp is Available",
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PackageDropdownMenuField(
                label = "Package",
                selected = state.selectedPackage,
                options = state.packageNameList.data.orEmpty(),
                onSelected = onPackageSelected,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = state.price,
                onValueChange = {},
                readOnly = true,
                label = { Text("Price") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
            Text(
                "Minimum Order ${state.selectedPackage?.displayPrice.orEmpty()}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.caption
            )
            Text(
                "The weight is 5kg",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.caption
            )
        }

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
                onValueChange = onNoteChanged,
                label = { Text("Note") },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .defaultMinSize(minWidth = 120.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit")
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

    Box(modifier) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
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

@Composable
fun DropdownMenuField(
    label: String,
    value: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(onClick = {
                    onOptionSelected(item)
                    expanded = false
                }) {
                    Text(item)
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
        selectedPackage = PackageItem("Regular", "Rp 5.000"),
        price = "Rp 25.000",
        paymentMethod = "Paid by Cash",
        note = "-",
        packageNameList = SectionState(data = listOf(PackageItem("Regular", "Rp 5.000,-"))),
        paymentOption = listOf("Paid by Cash", "Paid by QRIS")
    )

    OrderBottomSheet(
        state = state,
        onNameChanged = {},
        onPhoneChanged = {},
        onPackageSelected = {},
        onPaymentMethodSelected = {},
        onNoteChanged = {},
        onSubmit = {}
    )
}