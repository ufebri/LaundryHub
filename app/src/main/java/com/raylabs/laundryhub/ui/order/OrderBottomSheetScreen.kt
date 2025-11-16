package com.raylabs.laundryhub.ui.order

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
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
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.TextUtil.removeRupiahFormat
import com.raylabs.laundryhub.ui.common.util.WhatsAppHelper
import com.raylabs.laundryhub.ui.inventory.state.PackageItem
import com.raylabs.laundryhub.ui.order.state.OrderUiState
import com.raylabs.laundryhub.ui.order.state.isSubmitEnabled
import com.raylabs.laundryhub.ui.order.state.isUpdateEnabled
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.MaterialTheme as MaterialTheme3
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton

@OptIn(ExperimentalMaterial3Api::class)
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
                        imageVector = Icons.Default.Send,
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

        val context = LocalContext.current
        val orderDate = state.orderDate.ifBlank { "" }
        val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        val parsedInitialDate = remember(orderDate) {
            runCatching { if (orderDate.isNotBlank()) dateFormatter.parse(orderDate)?.time else null }
                .getOrNull()
        }
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = parsedInitialDate)
        val material2Colors = MaterialTheme.colors
        val isLightTheme = material2Colors.isLight
        val surfaceColor = if (isLightTheme) material2Colors.surface else material2Colors.background
        val onSurfaceColor =
            if (isLightTheme) material2Colors.onSurface else material2Colors.onBackground
        val primaryColor = material2Colors.primary
        val onPrimaryColor = material2Colors.onPrimary
        val secondaryColor = material2Colors.secondary
        val onSecondaryColor = material2Colors.onSecondary
        val backgroundColor = material2Colors.background
        val outlineColor = onSurfaceColor.copy(alpha = 0.3f)
        val dialogColorScheme = if (isLightTheme) {
            lightColorScheme(
                primary = primaryColor,
                onPrimary = onPrimaryColor,
                secondary = secondaryColor,
                onSecondary = onSecondaryColor,
                surface = surfaceColor,
                onSurface = onSurfaceColor,
                background = backgroundColor,
                onBackground = material2Colors.onBackground,
                outline = outlineColor
            )
        } else {
            darkColorScheme(
                primary = primaryColor,
                onPrimary = onPrimaryColor,
                secondary = secondaryColor,
                onSecondary = onSecondaryColor,
                surface = surfaceColor,
                onSurface = onSurfaceColor,
                background = backgroundColor,
                onBackground = material2Colors.onBackground,
                outline = outlineColor
            )
        }

        LaunchedEffect(state.orderDate) {
            parseDateMillis(state.orderDate, dateFormatter)?.let { millis ->
                if (datePickerState.selectedDateMillis != millis) {
                    datePickerState.selectedDateMillis = millis
                }
            }
        }

        if (showDatePicker) {
            MaterialTheme3(colorScheme = dialogColorScheme) {
                val headlineFormatter =
                    remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                val headlineText = datePickerState.selectedDateMillis?.let { millis ->
                    headlineFormatter.format(Date(millis))
                }
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        M3TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                onOrderDateSelected(dateFormatter.format(Date(it)))
                            }
                            showDatePicker = false
                        }) {
                            M3Text(text = context.getString(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        M3TextButton(onClick = { showDatePicker = false }) {
                            M3Text(text = context.getString(android.R.string.cancel))
                        }
                    }
                ) {
                    val mediumOnSurface = MaterialTheme3.colorScheme.onSurface.copy(alpha = 0.7f)
                    DatePicker(
                        state = datePickerState,
                        title = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                M3Text(
                                    text = context.getString(R.string.order_date),
                                    style = MaterialTheme3.typography.titleMedium,
                                    color = MaterialTheme3.colorScheme.onSurface
                                )
                            }
                        },
                        headline = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                M3Text(
                                    text = headlineText ?: context.getString(R.string.order_date),
                                    style = MaterialTheme3.typography.headlineMedium,
                                    color = MaterialTheme3.colorScheme.onSurface
                                )
                            }
                        },
                        colors = DatePickerDefaults.colors(
                            containerColor = MaterialTheme3.colorScheme.surface,
                            titleContentColor = MaterialTheme3.colorScheme.onSurface,
                            headlineContentColor = MaterialTheme3.colorScheme.onSurface,
                            weekdayContentColor = mediumOnSurface,
                            subheadContentColor = mediumOnSurface,
                            yearContentColor = MaterialTheme3.colorScheme.onSurface,
                            currentYearContentColor = MaterialTheme3.colorScheme.primary,
                            selectedYearContentColor = MaterialTheme3.colorScheme.onPrimary,
                            selectedYearContainerColor = MaterialTheme3.colorScheme.primary,
                            dayContentColor = MaterialTheme3.colorScheme.onSurface,
                            disabledDayContentColor = MaterialTheme3.colorScheme.onSurface.copy(
                                alpha = ContentAlpha.disabled
                            ),
                            selectedDayContentColor = MaterialTheme3.colorScheme.onPrimary,
                            selectedDayContainerColor = MaterialTheme3.colorScheme.primary,
                            todayContentColor = MaterialTheme3.colorScheme.primary,
                            todayDateBorderColor = MaterialTheme3.colorScheme.primary
                        )
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = orderDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Order Date") },
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showDatePicker = true
                    }
            )
        }

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

@Composable
fun DropdownMenuField(
    label: String,
    value: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxWidth()
            .clickable { expanded = true }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
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

private fun parseDateMillis(value: String?, formatter: SimpleDateFormat): Long? {
    if (value.isNullOrBlank()) return null
    return runCatching { formatter.parse(value)?.time }.getOrNull()
}
