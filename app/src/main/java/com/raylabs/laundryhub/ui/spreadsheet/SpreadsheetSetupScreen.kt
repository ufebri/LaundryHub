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
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.spreadsheet.state.SpreadsheetSetupUiState
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub
import com.raylabs.laundryhub.ui.theme.appAccentContainer
import com.raylabs.laundryhub.ui.theme.appBorderSoft
import com.raylabs.laundryhub.ui.theme.appCardSurface
import com.raylabs.laundryhub.ui.theme.appChromeTopTint
import com.raylabs.laundryhub.ui.theme.appErrorContainer
import com.raylabs.laundryhub.ui.theme.appErrorContent
import com.raylabs.laundryhub.ui.theme.appInfoContainer
import com.raylabs.laundryhub.ui.theme.appInfoContent
import com.raylabs.laundryhub.ui.theme.appMutedContainer
import com.raylabs.laundryhub.ui.theme.appMutedContent
import com.raylabs.laundryhub.ui.theme.appMutedInfoContainer
import com.raylabs.laundryhub.ui.theme.appMutedInfoContent
import com.raylabs.laundryhub.ui.theme.appPanelElevated
import com.raylabs.laundryhub.ui.theme.appPanelTranslucent
import com.raylabs.laundryhub.ui.theme.appScreenBackground
import com.raylabs.laundryhub.ui.theme.appScreenGradientBottom
import com.raylabs.laundryhub.ui.theme.appSuccessContainer
import com.raylabs.laundryhub.ui.theme.appSuccessContent
import com.raylabs.laundryhub.ui.theme.pill
import com.raylabs.laundryhub.ui.theme.sectionEyebrow
import com.raylabs.laundryhub.ui.theme.statusPill
import com.raylabs.laundryhub.ui.theme.stepBadge
import com.raylabs.laundryhub.ui.theme.surfaceHero
import com.raylabs.laundryhub.ui.theme.surfacePanel

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
    val colors = MaterialTheme.colors
    val topTint = colors.appChromeTopTint
    val screenBackground = colors.appScreenBackground
    val screenGradientBottom = colors.appScreenGradientBottom
    val cardBackground = colors.appCardSurface
    val cardBorder = colors.appBorderSoft
    val heroPanelBackground = colors.appPanelTranslucent
    val heroAccountBackground = colors.appPanelElevated
    val accentContainer = colors.appAccentContainer
    val successBackground = colors.appSuccessContainer
    val successContent = colors.appSuccessContent
    val lockedBackground = colors.appMutedContainer
    val lockedContent = colors.appMutedContent
    val infoBackground = colors.appInfoContainer
    val infoContent = colors.appInfoContent
    val mutedInfoBackground = colors.appMutedInfoContainer
    val mutedInfoContent = colors.appMutedInfoContent
    val errorBackground = colors.appErrorContainer
    val errorContent = colors.appErrorContent
    val requestAccessButtonBackground = colors.appCardSurface
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
                            screenGradientBottom
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
                borderColor = cardBorder,
                panelBackground = heroPanelBackground,
                accountBackground = heroAccountBackground,
                accentContainer = accentContainer
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
                    successBackground
                } else {
                    infoBackground
                },
                statusContentColor = if (isSheetsAccessReady) {
                    successContent
                } else {
                    infoContent
                },
                description = if (isSheetsAccessReady) {
                    stringResource(
                        R.string.google_sheets_access_ready,
                        connectedAccountEmail ?: stringResource(R.string.guest)
                    )
                } else {
                    stringResource(R.string.google_sheets_access_required)
                },
                borderColor = cardBorder,
                cardBackground = cardBackground,
                accentContainer = accentContainer
            ) {
                if (isSheetsAccessReady) {
                    SetupInlineMessage(
                        text = stringResource(R.string.spreadsheet_step_done),
                        backgroundColor = successBackground,
                        contentColor = successContent
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
                            contentColor = MaterialTheme.colors.onPrimary
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
                    infoBackground
                } else {
                    lockedBackground
                },
                statusContentColor = if (isSheetsAccessReady) {
                    infoContent
                } else {
                    lockedContent
                },
                description = if (isSheetsAccessReady) {
                    stringResource(R.string.spreadsheet_step_choose_sheet_description)
                } else {
                    stringResource(R.string.spreadsheet_step_choose_sheet_locked_description)
                },
                borderColor = cardBorder,
                cardBackground = cardBackground,
                accentContainer = accentContainer
            ) {
                if (!isSheetsAccessReady) {
                    SetupInlineMessage(
                        text = stringResource(R.string.spreadsheet_step_choose_sheet_locked_hint),
                        backgroundColor = mutedInfoBackground,
                        contentColor = mutedInfoContent
                    )
                } else {
                    if (!state.configuredSpreadsheetName.isNullOrBlank()) {
                        SetupInlineMessage(
                            text = stringResource(
                                R.string.current_spreadsheet_connected,
                                state.configuredSpreadsheetName
                            ),
                            backgroundColor = infoBackground,
                            contentColor = infoContent
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
                        shape = MaterialTheme.shapes.medium,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = PurpleLaundryHub,
                            focusedLabelColor = PurpleLaundryHub,
                            cursorColor = PurpleLaundryHub,
                            backgroundColor = cardBackground,
                            textColor = MaterialTheme.colors.onSurface,
                            unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.70f),
                            placeholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
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
                            backgroundColor = errorBackground,
                            contentColor = errorContent
                        )
                    }

                    if (!state.infoMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SetupInlineMessage(
                            text = state.infoMessage,
                            backgroundColor = infoBackground,
                            contentColor = infoContent
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
                            contentColor = MaterialTheme.colors.onPrimary
                        )
                    ) {
                        if (state.isValidating) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colors.onPrimary,
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
                    statusContainerColor = infoBackground,
                    statusContentColor = infoContent,
                    description = stringResource(R.string.spreadsheet_step_request_access_description),
                    borderColor = cardBorder,
                    cardBackground = cardBackground,
                    accentContainer = accentContainer
                ) {
                    SetupInlineMessage(
                        text = stringResource(R.string.spreadsheet_step_request_access_hint),
                        backgroundColor = infoBackground,
                        contentColor = infoContent
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenInGoogleSheets,
                        enabled = !state.isValidating && isSheetsAccessReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = requestAccessButtonBackground,
                            contentColor = infoContent,
                            disabledBackgroundColor = lockedBackground,
                            disabledContentColor = lockedContent
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
    borderColor: Color,
    panelBackground: Color,
    accountBackground: Color,
    accentContainer: Color
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
                    .clip(MaterialTheme.shapes.surfaceHero)
                    .background(panelBackground)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = MaterialTheme.shapes.surfaceHero
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
                    .clip(MaterialTheme.shapes.surfacePanel)
                    .background(accountBackground)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = MaterialTheme.shapes.surfacePanel
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentContainer),
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
                        .background(accentContainer),
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
    cardBackground: Color,
    accentContainer: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, borderColor),
        elevation = 3.dp,
        backgroundColor = cardBackground
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
                    .clip(MaterialTheme.shapes.medium)
                    .background(accentContainer),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon()

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(cardBackground)
                        .border(
                            width = 1.dp,
                            color = PurpleLaundryHub.copy(alpha = 0.14f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.stepBadge,
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
                            style = MaterialTheme.typography.sectionEyebrow,
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
            .clip(MaterialTheme.shapes.pill)
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.statusPill,
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
            .clip(MaterialTheme.shapes.medium)
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
