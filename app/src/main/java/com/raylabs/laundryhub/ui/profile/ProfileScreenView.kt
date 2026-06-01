package com.raylabs.laundryhub.ui.profile

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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.ui.common.dummy.profile.dummyProfileUiState
import com.raylabs.laundryhub.ui.component.AppConfirmationDialog
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAd
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.onboarding.LoginViewModel
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import com.raylabs.laundryhub.ui.reminder.formatReminderTime
import java.util.Locale

private val ProfileHeroColor = Color(0xFF31284B)
private val ProfileCardColor = Color(0xFF43365F)
private val ProfileCardColorSoft = Color(0xFF55447A)
private val ProfileCardLine = Color.White.copy(alpha = 0.12f)
private val ProfileBadgeFill = Color(0xFFE3D7FF)
private val ProfileMutedText = Color(0xFFE0D7F5)
private val ProfileContentPadding = 16.dp

@Composable
fun ProfileScreenView(
    viewModel: ProfileViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel,
    bannerState: InlineAdaptiveBannerAdState? = null,
    onInventoryClick: () -> Unit = {},
    onReminderSettingsClick: () -> Unit = {},
    onSyncSettingsClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val resolvedBannerState = bannerState ?: rememberInlineAdaptiveBannerAdState("profile_inline")

    Scaffold(
        topBar = { DefaultTopAppBar(stringResource(R.string.profile)) }
    ) { padding ->
        ProfileScreenContent(
            state = state,
            bannerState = resolvedBannerState,
            modifier = Modifier.padding(padding),
            onLoggedOut = {
                viewModel.logOut(onSuccess = {
                    loginViewModel.clearUser()
                })
            },
            onInventoryClick = onInventoryClick,
            onReminderSettingsClick = onReminderSettingsClick,
            onSyncSettingsClick = onSyncSettingsClick,
            onWhatsAppOptionChanged = viewModel::setShowWhatsAppOption,
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
    onReminderSettingsClick: () -> Unit = {},
    onSyncSettingsClick: () -> Unit = {},
    onWhatsAppOptionChanged: (Boolean) -> Unit = {},
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

            InlineAdaptiveBannerAd(state = bannerState)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = stringResource(R.string.settings))
                SettingsCard(
                    state = state,
                    cacheSizeText = cacheSizeText,
                    onReminderSettingsClick = onReminderSettingsClick,
                    onSyncSettingsClick = onSyncSettingsClick,
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
            AppConfirmationDialog(
                title = stringResource(R.string.clear_cache),
                message = stringResource(R.string.clear_cache_confirmation),
                confirmLabel = stringResource(R.string.clear),
                dismissLabel = stringResource(R.string.cancel),
                onConfirm = onConfirmClearCache,
                onDismiss = onDismissClearCache
            )
        }
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
        Row(
            modifier = Modifier.padding(20.dp),
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

@Composable
private fun SectionHeader(title: String) {
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
private fun SettingsCard(
    state: ProfileUiState,
    cacheSizeText: String,
    onReminderSettingsClick: () -> Unit,
    onSyncSettingsClick: () -> Unit,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onReminderSettingsClick)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileVectorBadge(imageVector = Icons.Default.Build)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reminder_section_title),
                        color = Color.White,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rememberReminderSettingsSummary(state.reminderSettings),
                        color = ProfileMutedText,
                        style = MaterialTheme.typography.caption
                    )
                }
            }

            Divider(color = ProfileCardLine)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSyncSettingsClick)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileVectorBadge(imageVector = Icons.Default.Refresh)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sync Master Data",
                        color = Color.White,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Check data differences before sync",
                        color = ProfileMutedText,
                        style = MaterialTheme.typography.caption
                    )
                }
            }

            Divider(color = ProfileCardLine)

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
                    CacheSizeBadge(cacheSizeText)
                }
            }
        }
    }
}

@Composable
private fun rememberReminderSettingsSummary(settings: ReminderSettings): String {
    val context = LocalContext.current

    if (!settings.isReminderEnabled) {
        return stringResource(R.string.reminder_settings_summary_off)
    }

    return if (settings.isDailyNotificationEnabled) {
        stringResource(
            R.string.reminder_settings_summary_on_with_time,
            formatReminderTime(
                context = context,
                hourOfDay = settings.notificationHour,
                minute = settings.notificationMinute
            )
        )
    } else {
        stringResource(R.string.reminder_settings_summary_on_without_notifications)
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
private fun CacheSizeBadge(cacheSizeText: String) {
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
        topBar = { DefaultTopAppBar(stringResource(R.string.profile)) }
    ) { padding ->
        ProfileScreenContent(
            state = dummyProfileUiState,
            bannerState = bannerState,
            modifier = Modifier.padding(padding),
            onReminderSettingsClick = {},
            onSyncSettingsClick = {}
        )
    }
}
