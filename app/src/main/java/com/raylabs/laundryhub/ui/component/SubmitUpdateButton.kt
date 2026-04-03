package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R

@Composable
fun SubmitUpdateButton(
    isEditMode: Boolean,
    isEnabled: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = true,
    buttonHeightDp: Int = 56,
    minWidthDp: Int = 120,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val actionLabel =
        if (isEditMode) stringResource(R.string.update) else stringResource(R.string.submit)
    val loadingLabel =
        if (isEditMode) stringResource(R.string.updating) else stringResource(R.string.submitting)
    val action = if (isEditMode) onUpdate else onSubmit

    val baseModifier = if (fillMaxWidth) {
        modifier.fillMaxWidth()
    } else {
        modifier
    }

    Button(
        onClick = action,
        enabled = isEnabled,
        modifier = baseModifier
            .height(buttonHeightDp.dp)
            .defaultMinSize(minWidth = minWidthDp.dp),
        shape = shape
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(loadingLabel)
        } else {
            Text(actionLabel)
        }
    }
}
