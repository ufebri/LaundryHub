package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import com.raylabs.laundryhub.ui.theme.appBorderSoft
import com.raylabs.laundryhub.ui.theme.appErrorContainer
import com.raylabs.laundryhub.ui.theme.appErrorContent
import com.raylabs.laundryhub.ui.theme.appInfoContainer
import com.raylabs.laundryhub.ui.theme.appInfoContent
import com.raylabs.laundryhub.ui.theme.modalSheetTop

private enum class InventoryPackageAction {
    Edit,
    Delete
}

@Composable
fun InventoryPackageActionSheet(
    visible: Boolean,
    packageItem: PackageItem?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible || packageItem == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.38f))
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onDismiss()
                }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            shape = MaterialTheme.shapes.modalSheetTop,
            color = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface,
            elevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
                            shape = CircleShape
                        )
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = packageItem.name,
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = packageItem.displayRate,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
                    )
                }

                InventoryPackageActionRow(
                    action = InventoryPackageAction.Edit,
                    onClick = onEdit
                )

                InventoryPackageActionRow(
                    action = InventoryPackageAction.Delete,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
fun InventoryPackageDeleteConfirmationSheet(
    visible: Boolean,
    packageItem: PackageItem?,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible || packageItem == null) return

    AppConfirmationSheet(
        title = stringResource(R.string.inventory_package_delete_confirmation_title),
        message = stringResource(
            R.string.inventory_package_delete_confirmation_message,
            packageItem.name
        ),
        confirmLabel = stringResource(
            if (isDeleting) R.string.deleting else R.string.delete
        ),
        dismissLabel = stringResource(R.string.cancel),
        onConfirm = {
            if (!isDeleting) onConfirm()
        },
        onDismiss = {
            if (!isDeleting) onDismiss()
        },
        modifier = modifier,
        icon = Icons.Default.Delete,
        bulletPoints = listOf(
            stringResource(R.string.inventory_package_delete_point_permanent),
            stringResource(R.string.inventory_package_delete_point_orders)
        )
    )
}

@Composable
fun InventoryPackageEditorSheet(
    visible: Boolean,
    isEditMode: Boolean,
    packageName: String,
    packagePrice: String,
    packageDuration: String,
    packageUnit: String,
    isSaveEnabled: Boolean,
    isSubmitting: Boolean,
    onPackageNameChange: (String) -> Unit,
    onPackagePriceChange: (String) -> Unit,
    onPackageDurationChange: (String) -> Unit,
    onPackageUnitChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.38f))
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isSubmitting) onDismiss()
                }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            shape = MaterialTheme.shapes.modalSheetTop,
            color = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface,
            elevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
                            shape = CircleShape
                        )
                )

                Text(
                    text = stringResource(
                        if (isEditMode) {
                            R.string.inventory_package_editor_update_title
                        } else {
                            R.string.inventory_package_editor_add_title
                        }
                    ),
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { onPackageNameChange(it.take(40)) },
                    label = { Text(stringResource(R.string.inventory_package_field_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    enabled = !isSubmitting
                )

                OutlinedTextField(
                    value = packagePrice,
                    onValueChange = { rawInput ->
                        onPackagePriceChange(rawInput.filter(Char::isDigit).take(8))
                    },
                    label = { Text(stringResource(R.string.inventory_package_field_price)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Text(
                            text = stringResource(R.string.currency_prefix_rupiah),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Number
                    ),
                    enabled = !isSubmitting
                )

                OutlinedTextField(
                    value = packageDuration,
                    onValueChange = { onPackageDurationChange(it.take(20)) },
                    label = { Text(stringResource(R.string.inventory_package_field_duration)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    enabled = !isSubmitting
                )

                Text(
                    text = stringResource(R.string.inventory_package_duration_hint),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f)
                )

                OutlinedTextField(
                    value = packageUnit,
                    onValueChange = { onPackageUnitChange(it.take(10)) },
                    label = { Text(stringResource(R.string.inventory_package_field_unit)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    enabled = !isSubmitting
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting,
                        border = BorderStroke(1.dp, MaterialTheme.colors.appBorderSoft)
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = isSaveEnabled && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(
                                when {
                                    isSubmitting && isEditMode -> R.string.updating
                                    isSubmitting -> R.string.inventory_package_adding
                                    isEditMode -> R.string.update
                                    else -> R.string.inventory_package_add
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryPackageActionRow(
    action: InventoryPackageAction,
    onClick: () -> Unit
) {
    val isDelete = action == InventoryPackageAction.Delete
    val title = if (isDelete) {
        stringResource(R.string.inventory_package_action_delete_title)
    } else {
        stringResource(R.string.inventory_package_action_edit_title)
    }

    val leadingContainerColor = if (isDelete) {
        MaterialTheme.colors.appErrorContainer
    } else {
        MaterialTheme.colors.appInfoContainer
    }
    val leadingContentColor = if (isDelete) {
        MaterialTheme.colors.appErrorContent
    } else {
        MaterialTheme.colors.appInfoContent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colors.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colors.appBorderSoft
        ),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(leadingContainerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDelete) Icons.Default.Delete else Icons.Default.Edit,
                    contentDescription = null,
                    tint = leadingContentColor
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.48f)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewInventoryPackageActionSheet() {
    LaundryHubTheme {
        InventoryPackageActionSheet(
            visible = true,
            packageItem = PackageItem(
                name = "Express - 6H",
                price = "10000",
                work = "6h",
                unit = "kg",
                sheetRowIndex = 2
            ),
            onEdit = {},
            onDelete = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun PreviewInventoryPackageEditorSheet() {
    LaundryHubTheme {
        InventoryPackageEditorSheet(
            visible = true,
            isEditMode = false,
            packageName = "Express - 6H",
            packagePrice = "10000",
            packageDuration = "6h",
            packageUnit = "kg",
            isSaveEnabled = true,
            isSubmitting = false,
            onPackageNameChange = {},
            onPackagePriceChange = {},
            onPackageDurationChange = {},
            onPackageUnitChange = {},
            onDismiss = {},
            onSave = {}
        )
    }
}
