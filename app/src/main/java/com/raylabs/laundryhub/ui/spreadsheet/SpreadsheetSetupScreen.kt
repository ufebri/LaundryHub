package com.raylabs.laundryhub.ui.spreadsheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.spreadsheet.state.SpreadsheetSetupUiState
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub

@Composable
fun SpreadsheetSetupScreen(
    state: SpreadsheetSetupUiState,
    connectedAccountEmail: String?,
    requiresGoogleSheetsAccess: Boolean,
    onInputChanged: (String) -> Unit,
    onValidate: () -> Unit,
    onOpenInGoogleSheets: () -> Unit,
    onSignOut: () -> Unit,
    onGrantGoogleSheetsAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSheetsAccessReady = !requiresGoogleSheetsAccess
    val topTint = Color(0xFFF1ECFF)
    val screenBackground = Color(0xFFFBFAFE)
    val cardBorder = PurpleLaundryHub.copy(alpha = 0.14f)
    val isPreview = LocalInspectionMode.current
    val view = LocalView.current
    val useDarkStatusIcons = topTint.luminance() > 0.5f

    if (!isPreview) {
        DisposableEffect(view, useDarkStatusIcons) {
            val window = (view.context as? android.app.Activity)?.window
            if (window == null) return@DisposableEffect onDispose {}
            val controller = WindowInsetsControllerCompat(window, view)
            val previousAppearance = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = useDarkStatusIcons
            onDispose {
                controller.isAppearanceLightStatusBars = previousAppearance
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = screenBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            topTint,
                            screenBackground,
                            Color.White
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SpreadsheetHeroSection(
                connectedAccountEmail = connectedAccountEmail,
                onSignOut = onSignOut,
                borderColor = cardBorder
            )

            SpreadsheetStepCard(
                stepNumber = 1,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_order),
                        contentDescription = null,
                        tint = PurpleLaundryHub
                    )
                },
                title = stringResource(R.string.spreadsheet_step_grant_access_title),
                statusLabel = if (isSheetsAccessReady) {
                    stringResource(R.string.spreadsheet_status_connected)
                } else {
                    stringResource(R.string.spreadsheet_status_needed)
                },
                statusContainerColor = if (isSheetsAccessReady) {
                    Color(0xFFE8F5E9)
                } else {
                    PurpleLaundryHub.copy(alpha = 0.12f)
                },
                statusContentColor = if (isSheetsAccessReady) {
                    Color(0xFF2E7D32)
                } else {
                    PurpleLaundryHub
                },
                description = if (isSheetsAccessReady) {
                    stringResource(
                        R.string.google_sheets_access_ready,
                        connectedAccountEmail ?: stringResource(R.string.guest)
                    )
                } else {
                    stringResource(R.string.google_sheets_access_required)
                },
                borderColor = cardBorder
            ) {
                if (isSheetsAccessReady) {
                    SetupInlineMessage(
                        text = stringResource(R.string.spreadsheet_step_done),
                        backgroundColor = Color(0xFFEFF8F1),
                        contentColor = Color(0xFF2E7D32)
                    )
                } else {
                    Button(
                        onClick = onGrantGoogleSheetsAccess,
                        enabled = !state.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = PurpleLaundryHub,
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.grant_google_sheets_access))
                    }
                }
            }

            SpreadsheetStepCard(
                stepNumber = 2,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_assignment),
                        contentDescription = null,
                        tint = PurpleLaundryHub
                    )
                },
                title = stringResource(R.string.spreadsheet_step_choose_sheet_title),
                statusLabel = if (isSheetsAccessReady) {
                    stringResource(R.string.spreadsheet_status_next)
                } else {
                    stringResource(R.string.spreadsheet_status_locked)
                },
                statusContainerColor = if (isSheetsAccessReady) {
                    Color(0xFFF1ECFF)
                } else {
                    Color(0xFFF2F2F6)
                },
                statusContentColor = if (isSheetsAccessReady) {
                    PurpleLaundryHub
                } else {
                    Color(0xFF6F6F76)
                },
                description = if (isSheetsAccessReady) {
                    stringResource(R.string.spreadsheet_step_choose_sheet_description)
                } else {
                    stringResource(R.string.spreadsheet_step_choose_sheet_locked_description)
                },
                borderColor = cardBorder
            ) {
                if (!isSheetsAccessReady) {
                    SetupInlineMessage(
                        text = stringResource(R.string.spreadsheet_step_choose_sheet_locked_hint),
                        backgroundColor = Color(0xFFF4F2F8),
                        contentColor = Color(0xFF5F5F67)
                    )
                } else {
                    if (!state.configuredSpreadsheetName.isNullOrBlank()) {
                        SetupInlineMessage(
                            text = stringResource(
                                R.string.current_spreadsheet_connected,
                                state.configuredSpreadsheetName
                            ),
                            backgroundColor = PurpleLaundryHub.copy(alpha = 0.08f),
                            contentColor = PurpleLaundryHub
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = state.input,
                        onValueChange = onInputChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.spreadsheet_input_label)) },
                        placeholder = { Text(stringResource(R.string.spreadsheet_input_placeholder)) },
                        enabled = !state.isBusy,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = PurpleLaundryHub,
                            focusedLabelColor = PurpleLaundryHub,
                            cursorColor = PurpleLaundryHub,
                            backgroundColor = Color.White
                        )
                    )

                    Text(
                        text = stringResource(R.string.spreadsheet_step_choose_sheet_hint),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    if (!state.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SetupInlineMessage(
                            text = state.errorMessage,
                            backgroundColor = Color(0xFFFFF0F0),
                            contentColor = MaterialTheme.colors.error
                        )
                    }

                    if (!state.infoMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SetupInlineMessage(
                            text = state.infoMessage,
                            backgroundColor = PurpleLaundryHub.copy(alpha = 0.08f),
                            contentColor = PurpleLaundryHub
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onValidate,
                        enabled = state.input.isNotBlank() && !state.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = PurpleLaundryHub,
                            contentColor = Color.White
                        )
                    ) {
                        if (state.isValidating) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(stringResource(R.string.spreadsheet_validating))
                            }
                        } else {
                            Text(stringResource(R.string.validate_and_continue))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.showRequestAccess) {
                SpreadsheetStepCard(
                    stepNumber = 3,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = null,
                            tint = PurpleLaundryHub
                        )
                    },
                    title = stringResource(R.string.spreadsheet_step_request_access_title),
                    statusLabel = stringResource(R.string.spreadsheet_status_needed),
                    statusContainerColor = PurpleLaundryHub.copy(alpha = 0.10f),
                    statusContentColor = PurpleLaundryHub,
                    description = stringResource(R.string.spreadsheet_step_request_access_description),
                    borderColor = cardBorder
                ) {
                    SetupInlineMessage(
                        text = stringResource(R.string.spreadsheet_step_request_access_hint),
                        backgroundColor = Color(0xFFF7F2FF),
                        contentColor = PurpleLaundryHub
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenInGoogleSheets,
                        enabled = !state.isValidating && isSheetsAccessReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.White,
                            contentColor = PurpleLaundryHub,
                            disabledBackgroundColor = Color(0xFFF2F2F6),
                            disabledContentColor = Color(0xFF8B8B95)
                        ),
                        border = BorderStroke(1.dp, cardBorder),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Text(stringResource(R.string.open_in_google_sheets))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpreadsheetHeroSection(
    connectedAccountEmail: String?,
    onSignOut: () -> Unit,
    borderColor: Color
) {
    val composition = rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lottie_report)
    ).value

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.connect_spreadsheet_title),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )

                Text(
                    text = stringResource(R.string.connect_spreadsheet_description),
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
                )
            }

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.74f))
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(74.dp)
                )
            }
        }

        if (!connectedAccountEmail.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PurpleLaundryHub.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_admin),
                        contentDescription = null,
                        tint = PurpleLaundryHub,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.connected_google_account_label),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f)
                    )
                    Text(
                        text = connectedAccountEmail,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PurpleLaundryHub.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = stringResource(R.string.use_another_google_account),
                            tint = PurpleLaundryHub,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpreadsheetStepCard(
    stepNumber: Int,
    leadingIcon: @Composable () -> Unit,
    title: String,
    statusLabel: String,
    statusContainerColor: Color,
    statusContentColor: Color,
    description: String,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, borderColor),
        elevation = 3.dp,
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PurpleLaundryHub.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon()

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = PurpleLaundryHub.copy(alpha = 0.14f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                        color = PurpleLaundryHub,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.spreadsheet_step_label, stepNumber),
                            style = MaterialTheme.typography.caption,
                            color = PurpleLaundryHub,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = title,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    SpreadsheetStatusPill(
                        label = statusLabel,
                        backgroundColor = statusContainerColor,
                        contentColor = statusContentColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                content()
            }
        }
    }
}

@Composable
private fun SpreadsheetStatusPill(
    label: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption.copy(fontSize = 11.sp),
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SetupInlineMessage(
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(contentColor)
        ) {
        }

        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            color = contentColor,
            fontWeight = FontWeight.Medium
        )
    }
}
