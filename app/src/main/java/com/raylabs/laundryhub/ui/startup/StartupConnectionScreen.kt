package com.raylabs.laundryhub.ui.startup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import com.raylabs.laundryhub.ui.theme.appAccentContainer
import com.raylabs.laundryhub.ui.theme.appBorderSoft
import com.raylabs.laundryhub.ui.theme.appErrorContainer
import com.raylabs.laundryhub.ui.theme.appErrorContent
import com.raylabs.laundryhub.ui.theme.appMutedContent
import com.raylabs.laundryhub.ui.theme.appScreenBackground
import com.raylabs.laundryhub.ui.theme.appScreenGradientBottom
import com.raylabs.laundryhub.ui.theme.pill

@Composable
fun StartupConnectionScreen(
    uiState: StartupConnectionUiState,
    onCheckAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState is StartupConnectionUiState.Ready) return

    val isChecking = uiState is StartupConnectionUiState.Checking
    val isRetrying = uiState is StartupConnectionUiState.Retrying
    val fallbackMessage = stringResource(R.string.startup_connection_unavailable_message)
    val title = if (isChecking) {
        stringResource(R.string.startup_connection_checking_title)
    } else {
        stringResource(R.string.startup_connection_unavailable_title)
    }
    val message = when (uiState) {
        is StartupConnectionUiState.Checking -> stringResource(R.string.startup_connection_checking_message)
        is StartupConnectionUiState.Maintenance -> uiState.message?.takeIf { it.isNotBlank() } ?: fallbackMessage
        is StartupConnectionUiState.Retrying,
        is StartupConnectionUiState.Unavailable -> fallbackMessage
        is StartupConnectionUiState.Ready -> ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colors.appScreenBackground,
                        MaterialTheme.colors.appScreenGradientBottom
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            StartupBrand()
            StartupStatusPanel(
                isChecking = isChecking,
                isRetrying = isRetrying,
                title = title,
                message = message,
                onCheckAgain = onCheckAgain
            )
        }
    }
}

@Composable
private fun StartupBrand() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = CircleShape,
            color = MaterialTheme.colors.appAccentContainer,
            elevation = 0.dp
        ) {
            Image(
                painter = painterResource(R.drawable.ic_branding),
                contentDescription = null,
                modifier = Modifier
                    .padding(14.dp)
                    .clip(CircleShape)
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Composable
private fun StartupStatusPanel(
    isChecking: Boolean,
    isRetrying: Boolean,
    title: String,
    message: String,
    onCheckAgain: () -> Unit
) {
    val panelColor = if (isChecking) {
        MaterialTheme.colors.appAccentContainer
    } else {
        MaterialTheme.colors.appErrorContainer
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = panelColor,
        border = BorderStroke(1.dp, MaterialTheme.colors.appBorderSoft),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StartupStatusIndicator(
                isChecking = isChecking,
                isRetrying = isRetrying
            )
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.appMutedContent,
                textAlign = TextAlign.Center
            )

            if (isChecking) {
                Text(
                    text = stringResource(R.string.startup_connection_checking_caption),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.appMutedContent,
                    textAlign = TextAlign.Center
                )
            } else {
                StartupRetryButton(
                    isRetrying = isRetrying,
                    onCheckAgain = onCheckAgain
                )
                Text(
                    text = stringResource(R.string.startup_connection_retry_hint),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.appMutedContent,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StartupStatusIndicator(
    isChecking: Boolean,
    isRetrying: Boolean
) {
    if (isChecking || isRetrying) {
        CircularProgressIndicator(
            modifier = Modifier.size(34.dp),
            strokeWidth = 3.dp,
            color = if (isChecking) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.appErrorContent
            }
        )
        return
    }

    Surface(
        modifier = Modifier.size(34.dp),
        shape = CircleShape,
        color = MaterialTheme.colors.appErrorContent.copy(alpha = 0.12f),
        elevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "!",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.appErrorContent
            )
        }
    }
}

@Composable
private fun StartupRetryButton(
    isRetrying: Boolean,
    onCheckAgain: () -> Unit
) {
    Button(
        onClick = onCheckAgain,
        enabled = !isRetrying,
        shape = MaterialTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        if (isRetrying) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colors.onPrimary
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isRetrying) {
                stringResource(R.string.startup_connection_checking_button)
            } else {
                stringResource(R.string.startup_connection_check_again)
            },
            style = MaterialTheme.typography.button
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStartupConnectionChecking() {
    LaundryHubTheme {
        StartupConnectionScreen(
            uiState = StartupConnectionUiState.Checking,
            onCheckAgain = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStartupConnectionUnavailable() {
    LaundryHubTheme {
        StartupConnectionScreen(
            uiState = StartupConnectionUiState.Unavailable,
            onCheckAgain = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStartupConnectionRetrying() {
    LaundryHubTheme {
        StartupConnectionScreen(
            uiState = StartupConnectionUiState.Retrying,
            onCheckAgain = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStartupConnectionMaintenance() {
    LaundryHubTheme {
        StartupConnectionScreen(
            uiState = StartupConnectionUiState.Maintenance("LaundryHub is being updated. Please check again soon."),
            onCheckAgain = {}
        )
    }
}
