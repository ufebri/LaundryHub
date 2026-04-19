package com.raylabs.laundryhub.ui.component

import androidx.annotation.RawRes
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.theme.appBorderSoft
import com.raylabs.laundryhub.ui.theme.modalSheetTop

@Composable
fun AppConfirmationSheet(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    @RawRes animationRes: Int? = null,
    icon: ImageVector? = null,
    bulletPoints: List<String> = emptyList()
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
                            shape = CircleShape
                        )
                )

                ConfirmationSheetVisual(
                    animationRes = animationRes,
                    icon = icon
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = message,
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center
                    )
                }

                if (bulletPoints.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bulletPoints.forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(6.dp)
                                        .background(
                                            MaterialTheme.colors.primary,
                                            shape = CircleShape
                                        )
                                )

                                Text(
                                    text = point,
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colors.appBorderSoft
                        )
                    ) {
                        Text(text = dismissLabel)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        )
                    ) {
                        Text(text = confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationSheetVisual(
    @RawRes animationRes: Int?,
    icon: ImageVector?
) {
    val isPreview = LocalInspectionMode.current
    if (animationRes == null || isPreview) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colors.primary.copy(alpha = if (MaterialTheme.colors.isLight) 0.14f else 0.24f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        return
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))

    Box(
        modifier = Modifier
            .size(96.dp)
            .background(
                MaterialTheme.colors.primary.copy(alpha = if (MaterialTheme.colors.isLight) 0.12f else 0.22f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(80.dp)
        )
    }
}

@Preview
@Composable
private fun PreviewAppConfirmationSheet() {
    AppConfirmationSheet(
        title = "Use another spreadsheet?",
        message = "You'll return to setup and reconnect another spreadsheet.",
        confirmLabel = "Continue",
        dismissLabel = "Not now",
        onConfirm = {},
        onDismiss = {},
        animationRes = R.raw.lottie_report,
        bulletPoints = listOf(
            "Current spreadsheet connection will be cleared",
            "Local app settings stay on this device"
        )
    )
}
