package com.raylabs.laundryhub.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.profile.dummyProfileUiState
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAd
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.onboarding.LoginViewModel
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import java.util.Locale

private val ProfileHeroColor = Color(0xFF31284B)
private val ProfileCardColor = Color(0xFF43365F)
private val ProfileCardColorSoft = Color(0xFF55447A)
private val ProfileCardLine = Color.White.copy(alpha = 0.12f)
private val ProfileBadgeFill = Color(0xFFE3D7FF)
private val ProfileMutedText = Color(0xFFE0D7F5)
private val ProfileSuccess = Color(0xFFB7F1C2)
private val ProfileWarning = Color(0xFFFFE2A4)
private val ProfileDangerSoft = Color(0xFFFFD0D0)
private val ProfileContentPadding = 16.dp

@Composable
fun ProfileScreenView(
    viewModel: ProfileViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel,
    bannerState: InlineAdaptiveBannerAdState? = null,
    onInventoryClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val resolvedBannerState = bannerState ?: rememberInlineAdaptiveBannerAdState("profile_inline")

    Scaffold(
        topBar = { DefaultTopAppBar("Profile") }
    ) { padding ->
        ProfileScreenContent(
            state = state,
            bannerState = resolvedBannerState,
            modifier = Modifier.padding(padding),
            onLoggedOut = {
                viewModel.logOut(onSuccess = {
                    // Handle successful logout, e.g., navigate to login screen
                    loginViewModel.clearUser()
                })
            },
            onInventoryClick = onInventoryClick,
            onWhatsAppOptionChanged = viewModel::setShowWhatsAppOption,
            onRevalidateSpreadsheet = viewModel::revalidateSpreadsheet,
            onChangeSpreadsheetClick = viewModel::openChangeSpreadsheetDialog,
            onConfirmChangeSpreadsheet = viewModel::confirmChangeSpreadsheet,
            onDismissChangeSpreadsheet = viewModel::dismissChangeSpreadsheetDialog,
            onClearCacheClick = viewModel::openClearCacheDialog,
            onConfirmClearCache = viewModel::clearCache,
            onDismissClearCache = viewModel::dismissClearCacheDialog
        )
    }
}

@Composable
fun ProfileScreenContent(
    state: ProfileUiState,
    bannerState: InlineAdaptiveBannerAdState,
    modifier: Modifier = Modifier,
    onLoggedOut: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onWhatsAppOptionChanged: (Boolean) -> Unit = {},
    onRevalidateSpreadsheet: () -> Unit = {},
    onChangeSpreadsheetClick: () -> Unit = {},
    onConfirmChangeSpreadsheet: () -> Unit = {},
    onDismissChangeSpreadsheet: () -> Unit = {},
    onClearCacheClick: () -> Unit = {},
    onConfirmClearCache: () -> Unit = {},
    onDismissClearCache: () -> Unit = {}
) {
    val cacheSizeText = when {
        state.cacheSize.isLoading -> stringResource(R.string.cache_size_loading)
        state.cacheSize.errorMessage != null -> stringResource(R.string.cache_size_unavailable)
        else -> formatCacheSize(state.cacheSize.data)
    }
    LaunchedEffect(state.logout.data) {
        if (state.logout.data == true) {
            onLoggedOut()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = ProfileContentPadding, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ProfileHeroCard(state = state)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(title = stringResource(R.string.store_title))
            ProfileActionCard(
                title = stringResource(R.string.inventory_title),
                description = stringResource(R.string.inventory_description),
                iconRes = R.drawable.ic_admin,
                onClick = onInventoryClick
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(title = stringResource(R.string.spreadsheet_settings))
            SpreadsheetManagementCard(
                state = state,
                onRevalidateSpreadsheet = onRevalidateSpreadsheet,
                onChangeSpreadsheetClick = onChangeSpreadsheetClick
            )
        }

        InlineAdaptiveBannerAd(state = bannerState)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(title = stringResource(R.string.settings))
            SettingsCard(
                state = state,
                cacheSizeText = cacheSizeText,
                onWhatsAppOptionChanged = onWhatsAppOptionChanged,
                onClearCacheClick = onClearCacheClick
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(title = stringResource(R.string.account_section_title))
            AccountCard(
                state = state,
                onLoggedOut = onLoggedOut
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (state.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearCache,
            title = { Text(text = stringResource(R.string.clear_cache)) },
            text = { Text(text = stringResource(R.string.clear_cache_confirmation)) },
            confirmButton = {
                TextButton(onClick = onConfirmClearCache) {
                    Text(text = stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearCache) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    if (state.showChangeSpreadsheetDialog) {
        AlertDialog(
            onDismissRequest = onDismissChangeSpreadsheet,
            title = { Text(text = stringResource(R.string.change_spreadsheet)) },
            text = {
                Text(text = stringResource(R.string.change_spreadsheet_confirmation))
            },
            confirmButton = {
                TextButton(onClick = onConfirmChangeSpreadsheet) {
                    Text(text = stringResource(R.string.change_spreadsheet))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissChangeSpreadsheet) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileHeroCard(state: ProfileUiState) {
    Card(
        backgroundColor = ProfileHeroColor,
        shape = RoundedCornerShape(24.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = state.user.data?.photoUrl,
                    error = painterResource(R.drawable.ic_branding),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.user.data?.displayName ?: stringResource(R.string.guest),
                        color = Color.White,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.user.data?.email.orEmpty(),
                        color = ProfileMutedText,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.onBackground
    )
}

@Composable
private fun ProfileActionCard(
    title: String,
    description: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Card(
        backgroundColor = ProfileCardColor,
        shape = RoundedCornerShape(22.dp),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileIconBadge(painterRes = iconRes)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = ProfileMutedText,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
private fun SpreadsheetManagementCard(
    state: ProfileUiState,
    onRevalidateSpreadsheet: () -> Unit,
    onChangeSpreadsheetClick: () -> Unit
) {
    val connectedSpreadsheet = state.connectedSpreadsheet.data
    val statusText = if (connectedSpreadsheet == null) {
        stringResource(R.string.spreadsheet_status_not_connected)
    } else {
        stringResource(R.string.spreadsheet_status_connected)
    }
    val statusBackground = if (connectedSpreadsheet == null) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFF315B43)
    }
    val statusTextColor = if (connectedSpreadsheet == null) {
        ProfileMutedText
    } else {
        ProfileSuccess
    }
    val validationMessage = state.spreadsheetValidation.errorMessage ?: state.spreadsheetValidation.data
    val validationColor = if (state.spreadsheetValidation.errorMessage != null) {
        ProfileDangerSoft
    } else {
        ProfileSuccess
    }
    val validationBackground = if (state.spreadsheetValidation.errorMessage != null) {
        Color(0xFF5C3740)
    } else {
        Color(0xFF314E3F)
    }

    Card(
        backgroundColor = ProfileCardColor,
        shape = RoundedCornerShape(22.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                ProfileIconBadge(painterRes = R.drawable.ic_assignment)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = connectedSpreadsheet?.spreadsheetName
                            ?: stringResource(R.string.spreadsheet_not_connected),
                        color = Color.White,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = connectedSpreadsheet?.let {
                            stringResource(R.string.spreadsheet_id_label, it.shortSpreadsheetId)
                        } ?: stringResource(R.string.spreadsheet_not_connected_description),
                        color = ProfileMutedText,
                        style = MaterialTheme.typography.caption
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = statusBackground,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusTextColor,
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!validationMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = validationBackground,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = validationMessage,
                        color = validationColor,
                        style = MaterialTheme.typography.caption
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRevalidateSpreadsheet,
                    enabled = !state.spreadsheetValidation.isLoading && connectedSpreadsheet != null,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ProfileCardColorSoft,
                        disabledBackgroundColor = ProfileCardColorSoft.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.spreadsheetValidation.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.revalidate_spreadsheet),
                            color = Color.White
                        )
                    }
                }

                OutlinedButton(
                    onClick = onChangeSpreadsheetClick,
                    enabled = connectedSpreadsheet != null,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    colors = darkOutlinedButtonColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.change_spreadsheet_short),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    state: ProfileUiState,
    cacheSizeText: String,
    onWhatsAppOptionChanged: (Boolean) -> Unit,
    onClearCacheClick: () -> Unit
) {
    Card(
        backgroundColor = ProfileCardColor,
        shape = RoundedCornerShape(22.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileVectorBadge(imageVector = Icons.Default.Build)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.whatsapp_option),
                            color = Color.White,
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.whatsapp_option_description),
                            color = ProfileMutedText,
                            style = MaterialTheme.typography.caption
                        )
                    }
                }

                Switch(
                    checked = state.showWhatsAppOption,
                    onCheckedChange = onWhatsAppOptionChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ProfileBadgeFill,
                        checkedTrackColor = ProfileCardColorSoft,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.35f)
                    )
                )
            }

            Divider(color = ProfileCardLine)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !state.clearCache.isLoading,
                        onClick = onClearCacheClick
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileVectorBadge(imageVector = Icons.Default.Build)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.clear_cache),
                        color = Color.White,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.clear_cache_description),
                        color = ProfileMutedText,
                        style = MaterialTheme.typography.caption
                    )
                }

                if (state.clearCache.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                color = ProfileCardColorSoft,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cacheSizeText,
                            color = Color.White,
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    state: ProfileUiState,
    onLoggedOut: () -> Unit
) {
    Card(
        backgroundColor = ProfileCardColor,
        shape = RoundedCornerShape(22.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_version),
                    color = ProfileMutedText,
                    style = MaterialTheme.typography.body2
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    color = Color.White,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onLoggedOut,
                enabled = !state.logout.isLoading,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ProfileCardColorSoft,
                    disabledBackgroundColor = ProfileCardColorSoft.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.logout.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sign_out),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileIconBadge(painterRes: Int) {
    Card(
        backgroundColor = ProfileBadgeFill,
        shape = CircleShape,
        elevation = 0.dp,
        modifier = Modifier.size(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = painterRes),
                contentDescription = null,
                tint = ProfileCardColor
            )
        }
    }
}

@Composable
private fun ProfileVectorBadge(imageVector: ImageVector) {
    Card(
        backgroundColor = ProfileBadgeFill,
        shape = CircleShape,
        elevation = 0.dp,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = ProfileCardColor
            )
        }
    }
}

@Composable
private fun darkOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    backgroundColor = Color.Transparent,
    contentColor = Color.White,
    disabledContentColor = Color.White.copy(alpha = 0.42f)
)

private fun formatCacheSize(sizeBytes: Long?): String {
    if (sizeBytes == null) return "-"
    val megaBytes = sizeBytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.getDefault(), "%.1f MB", megaBytes)
}

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    val bannerState = rememberInlineAdaptiveBannerAdState("preview_profile_inline")
    Scaffold(
        topBar = { DefaultTopAppBar("Profile") }
    ) { padding ->
        ProfileScreenContent(
            state = dummyProfileUiState,
            bannerState = bannerState,
            modifier = Modifier.padding(padding)
        )
    }
}
