package com.raylabs.laundryhub.ui.component

import android.app.DatePickerDialog
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.util.DateUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DatePickerField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    dateFormat: String = DateUtil.STANDARD_DATE_FORMATED
) {
    val context = LocalContext.current

    val dateFormatter = remember(dateFormat) {
        SimpleDateFormat(dateFormat, Locale.getDefault())
    }
    val parsedInitialDate = remember(value, dateFormat) {
        DateUtil.parseDate(value, dateFormat)?.time
    }
    val onDateSelectedState = rememberUpdatedState(newValue = onDateSelected)
    val openDialog = rememberSaveable { mutableStateOf(false) }

    if (openDialog.value) {
        DisposableEffect(Unit) {
            val initialCalendar = Calendar.getInstance().apply {
                timeInMillis = parsedInitialDate ?: System.currentTimeMillis()
            }
            val dialog = DatePickerDialog(
                context,
                R.style.Theme_LaundryHub_DatePicker,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onDateSelectedState.value(dateFormatter.format(selectedCalendar.time))
                },
                initialCalendar.get(Calendar.YEAR),
                initialCalendar.get(Calendar.MONTH),
                initialCalendar.get(Calendar.DAY_OF_MONTH)
            )

            dialog.setOnShowListener {
                val primaryColor = ContextCompat.getColor(context, R.color.colorPrimary)
                val res = context.resources

                fun setHeaderTextColor(idName: String) {
                    val id = res.getIdentifier(idName, "id", "android")
                    if (id != 0) {
                        val view = dialog.findViewById<View?>(id)
                        if (view is TextView) {
                            view.setTextColor(primaryColor)
                        }
                    }
                }

                setHeaderTextColor("date_picker_header_date")
                setHeaderTextColor("date_picker_header_year")
            }
            dialog.setOnDismissListener { openDialog.value = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
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
                    openDialog.value = true
                }
        )
    }
}
