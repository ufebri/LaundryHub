package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.raylabs.laundryhub.ui.theme.appAccentContainer
import com.raylabs.laundryhub.ui.theme.appBorderSoft
import com.raylabs.laundryhub.ui.theme.appMutedInfoContainer
import com.raylabs.laundryhub.ui.theme.appMutedInfoContent
import com.raylabs.laundryhub.ui.theme.modalSheetTop

@Composable
fun SingleSelectChipRow(
    label: String,
    options: List<String>,
    selectedValue: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                val isSelected = option == selectedValue
                Surface(
                    modifier = Modifier.clickable { onOptionSelected(option) },
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.surface
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.appBorderSoft
                    ),
                    elevation = 0.dp
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.body2,
                        color = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun <T> HorizontalSelectionCards(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    optionTitle: (T) -> String,
    modifier: Modifier = Modifier,
    optionSupportingText: (T) -> String? = { null },
    optionTrailingText: (T) -> String? = { null }
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 24.dp)
        ) {
            items(options) { option ->
                val isSelected = option == selectedOption
                val supportingText = optionSupportingText(option)
                val trailingText = optionTrailingText(option)
                val cardBackground = if (isSelected) {
                    MaterialTheme.colors.appAccentContainer
                } else {
                    MaterialTheme.colors.surface
                }

                Surface(
                    modifier = Modifier
                        .width(208.dp)
                        .semantics {
                            contentDescription = "$label option ${optionTitle(option)}"
                        }
                        .clickable { onOptionSelected(option) },
                    shape = MaterialTheme.shapes.medium,
                    color = cardBackground,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.appBorderSoft
                    ),
                    elevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(min = 84.dp)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = optionTitle(option),
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (isSelected) {
                                            MaterialTheme.colors.primary
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colors.primary
                                        } else {
                                            MaterialTheme.colors.appBorderSoft
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!supportingText.isNullOrBlank()) {
                                Text(
                                    text = supportingText,
                                    style = MaterialTheme.typography.subtitle2,
                                    color = if (isSelected) {
                                        MaterialTheme.colors.primary
                                    } else {
                                        MaterialTheme.colors.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }

                            if (!trailingText.isNullOrBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) {
                                        MaterialTheme.colors.primary.copy(alpha = 0.14f)
                                    } else {
                                        MaterialTheme.colors.appMutedInfoContainer
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) {
                                            MaterialTheme.colors.primary.copy(alpha = 0.18f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                ) {
                                    Text(
                                        text = trailingText,
                                        style = MaterialTheme.typography.caption,
                                        color = if (isSelected) {
                                            MaterialTheme.colors.primary
                                        } else {
                                            MaterialTheme.colors.appMutedInfoContent
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (supportingText.isNullOrBlank() && trailingText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SelectionSheetInlineOverlay(
    visible: Boolean,
    title: String,
    options: List<T>,
    selectedOption: T?,
    onDismiss: () -> Unit,
    onOptionSelected: (T) -> Unit,
    optionTitle: (T) -> String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    sheetMaxHeight: Dp = 560.dp,
    optionSupportingText: (T) -> String? = { null }
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
                    onDismiss()
                }
        )

        SelectionSheetPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            title = title,
            options = options,
            selectedOption = selectedOption,
            onDismiss = onDismiss,
            onOptionSelected = onOptionSelected,
            optionTitle = optionTitle,
            supportingText = supportingText,
            sheetMaxHeight = sheetMaxHeight,
            optionSupportingText = optionSupportingText
        )
    }
}

@Composable
fun <T> SelectionSheetPanel(
    title: String,
    options: List<T>,
    selectedOption: T?,
    onDismiss: () -> Unit,
    onOptionSelected: (T) -> Unit,
    optionTitle: (T) -> String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    sheetMaxHeight: Dp = 560.dp,
    optionSupportingText: (T) -> String? = { null }
) {
    Surface(
        modifier = modifier
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
                .heightIn(max = sheetMaxHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null
                        )
                    }
                }

                if (!supportingText.isNullOrBlank()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
                    )
                }
            }

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(options) { option ->
                    val isSelected = option == selectedOption
                    val optionDetails = optionSupportingText(option)
                    val containerColor = if (isSelected) {
                        MaterialTheme.colors.primary.copy(alpha = if (MaterialTheme.colors.isLight) 0.12f else 0.24f)
                    } else {
                        MaterialTheme.colors.surface
                    }
                    val borderColor = if (isSelected) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.appBorderSoft
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismiss()
                            },
                        shape = MaterialTheme.shapes.medium,
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor),
                        elevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        color = if (isSelected) {
                                            MaterialTheme.colors.primary
                                        } else {
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.18f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = optionTitle(option),
                                    style = MaterialTheme.typography.body1,
                                    color = MaterialTheme.colors.onSurface
                                )

                                if (!optionDetails.isNullOrBlank()) {
                                    Text(
                                        text = optionDetails,
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SelectionBottomSheetOverlay(
    visible: Boolean,
    title: String,
    options: List<T>,
    selectedOption: T?,
    onDismiss: () -> Unit,
    onOptionSelected: (T) -> Unit,
    optionTitle: (T) -> String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    sheetMaxHeight: Dp = 560.dp,
    optionSupportingText: (T) -> String? = { null }
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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

            SelectionSheetPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                title = title,
                options = options,
                selectedOption = selectedOption,
                onDismiss = onDismiss,
                onOptionSelected = onOptionSelected,
                optionTitle = optionTitle,
                supportingText = supportingText,
                sheetMaxHeight = sheetMaxHeight,
                optionSupportingText = optionSupportingText
            )
        }
    }
}
