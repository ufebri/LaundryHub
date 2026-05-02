package com.raylabs.laundryhub.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.onboarding.LoginViewModel
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import com.raylabs.laundryhub.ui.profile.state.UserItem
import com.raylabs.laundryhub.ui.component.*

@Composable
fun ProfileScreenView(
    viewModel: ProfileViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel,
    bannerState: InlineAdaptiveBannerAdState? = null,
    onInventoryClick: () -> Unit = {},
    onReminderSettingsClick: () -> Unit = {}
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
                    loginViewModel.clearUser()
                })
            },
            onInventoryClick = onInventoryClick,
            onReminderSettingsClick = onReminderSettingsClick,
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

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            ProfileUserSection(state.user.data)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfileMenuSection(
                onInventoryClick = onInventoryClick,
                onReminderSettingsClick = onReminderSettingsClick,
                onWhatsAppOptionChanged = onWhatsAppOptionChanged,
                showWhatsAppOption = state.showWhatsAppOption,
                cacheSizeText = cacheSizeText,
                onClearCacheClick = onClearCacheClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            InlineAdaptiveBannerAd(bannerState)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onLoggedOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White)
            ) {
                Text("Logout")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (state.showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = onDismissClearCache,
                title = { Text("Clear Cache") },
                text = { Text("Are you sure you want to clear the app cache?") },
                confirmButton = {
                    TextButton(onClick = onConfirmClearCache) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = onDismissClearCache) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ProfileUserSection(user: UserItem?) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = user?.displayName ?: "User", style = MaterialTheme.typography.h6)
        Text(text = user?.email ?: "", style = MaterialTheme.typography.body2)
    }
}

@Composable
fun ProfileMenuSection(
    onInventoryClick: () -> Unit,
    onReminderSettingsClick: () -> Unit,
    onWhatsAppOptionChanged: (Boolean) -> Unit,
    showWhatsAppOption: Boolean,
    cacheSizeText: String,
    onClearCacheClick: () -> Unit
) {
    Column {
        ListItem(text = { Text("Inventory") }, modifier = Modifier.clickable { onInventoryClick() })
        ListItem(text = { Text("Reminder Settings") }, modifier = Modifier.clickable { onReminderSettingsClick() })
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Show WhatsApp Option")
            Switch(checked = showWhatsAppOption, onCheckedChange = onWhatsAppOptionChanged)
        }

        ListItem(
            text = { Text("App Cache") },
            secondaryText = { Text(cacheSizeText) },
            modifier = Modifier.clickable { onClearCacheClick() }
        )
    }
}

fun formatCacheSize(bytes: Long?): String {
    if (bytes == null) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) "%.2f MB".format(mb) else "%.2f KB".format(kb)
}

@Composable
fun ListItem(text: @Composable () -> Unit, secondaryText: (@Composable () -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        text()
        if (secondaryText != null) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                secondaryText()
            }
        }
    }
}
