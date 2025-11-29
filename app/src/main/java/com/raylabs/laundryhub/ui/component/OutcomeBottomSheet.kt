package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.util.TextUtil.removeRupiahFormat
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState
import com.raylabs.laundryhub.ui.outcome.state.isSubmitEnabled
import com.raylabs.laundryhub.ui.outcome.state.isUpdateEnabled

@Composable
fun OutcomeBottomSheet(
    state: OutcomeUiState,
    onPurposeChanged: (String) -> Unit = {},
    onPriceChanged: (String) -> Unit = {},
    onPaymentMethodSelected: (String) -> Unit = {},
    onRemarkChanged: (String) -> Unit = {},
    onDateSelected: (String) -> Unit = {},
    onUpdate: () -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(unbounded = true)
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text =
                if (state.isEditMode) stringResource(R.string.update_outcome) else stringResource(R.string.add_outcome),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        DatePickerField(
            label = stringResource(R.string.Date),
            value = state.date,
            onDateSelected = onDateSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = { if (it.length <= 30) onPurposeChanged(it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            label = { Text(stringResource(R.string.purpose)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            label = { Text(stringResource(R.string.price)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Text("Rp", modifier = Modifier.padding(start = 4.dp))
            },
            trailingIcon = {
                Text(",-", modifier = Modifier.padding(end = 4.dp))
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuField(
            label = stringResource(R.string.payment_method),
            value = state.paymentStatus,
            options = state.paymentOption,
            onOptionSelected = onPaymentMethodSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.remark,
            onValueChange = { if (it.length <= 30) onRemarkChanged(it) },
            label = { Text(stringResource(R.string.remark)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        SubmitUpdateButton(
            isEditMode = state.isEditMode,
            isEnabled = if (state.isEditMode)
                state.isUpdateEnabled && !state.isSubmitting
            else
                state.isSubmitEnabled && !state.isSubmitting,
            isSubmitting = state.isSubmitting,
            onSubmit = onSubmit,
            onUpdate = onUpdate
        )
    }
}

@Composable
@Preview
fun PreviewOutcomeBottomSheet() {
    val state = OutcomeUiState()
    OutcomeBottomSheet(
        state = state
    )
}
