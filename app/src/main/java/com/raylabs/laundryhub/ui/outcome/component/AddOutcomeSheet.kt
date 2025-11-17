package com.raylabs.laundryhub.ui.outcome.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.outcome.state.OutcomeFormState
import com.raylabs.laundryhub.ui.outcome.state.paymentOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOutcomeSheet(
    formState: OutcomeFormState,
    onDateSelected: (String) -> Unit,
    onPurposeChanged: (String) -> Unit,
    onPriceChanged: (String) -> Unit,
    onPaymentSelected: (String) -> Unit,
    onRemarkChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val initialDateMillis = remember(formState.date) {
        parseMillis(formState.date)
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val formatted = dateFormatter.format(Date(selectedMillis))
                            onDateSelected(formatted)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.outcome_sheet_select))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(id = R.string.outcome_sheet_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.outcome_sheet_title),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        val dateInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = dateInteraction,
                    indication = null
                ) { showDatePicker = true }
        ) {
            OutlinedTextField(
                value = formState.formattedDate,
                onValueChange = {},
                readOnly = true,
                label = { Text(text = stringResource(id = R.string.outcome_sheet_date_label)) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource(id = R.string.outcome_sheet_date_icon)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = formState.purpose,
            onValueChange = { if (it.length <= 30) onPurposeChanged(it) },
            singleLine = true,
            label = { Text(text = stringResource(id = R.string.outcome_sheet_purpose_label)) },
            trailingIcon = {
                if (formState.purpose.isNotBlank()) {
                    IconButton(onClick = { onPurposeChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.outcome_sheet_clear_purpose)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = formState.priceDisplay,
            onValueChange = { input ->
                onPriceChanged(input.filter { it.isDigit() })
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Number
            ),
            label = { Text(text = stringResource(id = R.string.outcome_sheet_price_label)) },
            leadingIcon = { Text(text = stringResource(id = R.string.outcome_sheet_price_prefix)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        PaymentDropdownField(
            value = formState.payment,
            options = formState.paymentOptions,
            onSelected = onPaymentSelected
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = formState.remark,
            onValueChange = { if (it.length <= 30) onRemarkChanged(it) },
            label = { Text(text = stringResource(id = R.string.outcome_sheet_remark_label)) },
            trailingIcon = {
                if (formState.remark.isNotBlank()) {
                    IconButton(onClick = { onRemarkChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.outcome_sheet_clear_remark)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            enabled = formState.isValid && !formState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (formState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(id = R.string.outcome_sheet_submit))
            }
        }
    }
}

@Composable
private fun PaymentDropdownField(
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = stringResource(id = R.string.outcome_sheet_payment_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(id = R.string.outcome_sheet_payment_dropdown_icon)
                    )
                },
                enabled = false
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { item ->
                    DropdownMenuItem(
                        onClick = {
                            onSelected(item)
                            expanded = false
                        },
                        text = { Text(text = item) }
                    )
                }
            }
        }
    }
}

private fun parseMillis(value: String): Long? {
    return try {
        val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        parser.parse(value)?.time
    } catch (_: Exception) {
        null
    }
}
