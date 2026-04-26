package com.raylabs.laundryhub.ui.profile.inventory

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.ui.common.dummy.inventory.dummyInventoryUiState
import com.raylabs.laundryhub.ui.common.util.showQuickSnackbar
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAd
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.component.InventoryPackageActionSheet
import com.raylabs.laundryhub.ui.component.InventoryPackageDeleteConfirmationSheet
import com.raylabs.laundryhub.ui.component.InventoryPackageEditorSheet
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.profile.inventory.state.InventoryUiState
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import com.raylabs.laundryhub.ui.theme.appBorderSoft
import com.raylabs.laundryhub.ui.theme.appCardSurface
import com.raylabs.laundryhub.ui.theme.appMutedContainer
import com.raylabs.laundryhub.ui.theme.appMutedContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InventoryScreenView(
    viewModel: InventoryViewModel = hiltViewModel(),
    bannerState: InlineAdaptiveBannerAdState? = null,
    onBackClick: (() -> Unit)? = null
) {
    val state = viewModel.uiState
    val scaffoldState = rememberScaffoldState()
    val resolvedBannerState = bannerState ?: rememberInlineAdaptiveBannerAdState("inventory_inline")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isSavingPackage = state.savePackage.isLoading
    val isDeletingPackage = state.deletePackage.isLoading
    var selectedPackage by remember { mutableStateOf<PackageItem?>(null) }
    var packageEditorState by remember { mutableStateOf<InventoryPackageEditorState?>(null) }
    var pendingDeletePackage by remember { mutableStateOf<PackageItem?>(null) }

    LaunchedEffect(state.packages.errorMessage, state.otherPackages.errorMessage) {
        listOfNotNull(state.packages.errorMessage, state.otherPackages.errorMessage)
            .firstOrNull()
            ?.let { message ->
                scaffoldState.snackbarHostState.showQuickSnackbar(message)
            }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            DefaultTopAppBar(
                title = stringResource(R.string.inventory_title),
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(hostState = scaffoldState.snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            InventoryContent(
                state = state,
                bannerState = resolvedBannerState,
                modifier = Modifier.fillMaxSize(),
                isRefreshing = state.packages.isLoading || state.otherPackages.isLoading,
                onRefresh = viewModel::refreshInventory,
                onAddPackage = {
                    packageEditorState = InventoryPackageEditorState()
                },
                onPackageClick = { item ->
                    selectedPackage = item
                },
                onSuggestedPackageClick = { label ->
                    packageEditorState = InventoryPackageEditorState(
                        name = normalizeInventoryLabel(label)
                    )
                }
            )

            InventoryPackageActionSheet(
                visible = selectedPackage != null,
                packageItem = selectedPackage,
                onEdit = {
                    packageEditorState = selectedPackage?.toEditorState()
                    selectedPackage = null
                },
                onDelete = {
                    pendingDeletePackage = selectedPackage
                    selectedPackage = null
                },
                onDismiss = { selectedPackage = null }
            )

            InventoryPackageDeleteConfirmationSheet(
                visible = pendingDeletePackage != null,
                packageItem = pendingDeletePackage,
                isDeleting = isDeletingPackage,
                onConfirm = {
                    val item = pendingDeletePackage ?: return@InventoryPackageDeleteConfirmationSheet
                    coroutineScope.launch {
                        viewModel.deletePackage(
                            sheetRowIndex = item.sheetRowIndex,
                            onComplete = {
                                pendingDeletePackage = null
                                scaffoldState.snackbarHostState.showQuickSnackbar(
                                    context.getString(
                                        R.string.inventory_package_delete_success,
                                        item.name
                                    )
                                )
                            },
                            onError = { message ->
                                scaffoldState.snackbarHostState.showQuickSnackbar(
                                    message.ifBlank {
                                        context.getString(R.string.inventory_package_delete_failed)
                                    }
                                )
                            }
                        )
                    }
                },
                onDismiss = {
                    if (!isDeletingPackage) {
                        pendingDeletePackage = null
                    }
                }
            )

            InventoryPackageEditorSheet(
                visible = packageEditorState != null,
                isEditMode = packageEditorState?.isEditMode == true,
                packageName = packageEditorState?.name.orEmpty(),
                packagePrice = packageEditorState?.price.orEmpty(),
                packageDuration = packageEditorState?.duration.orEmpty(),
                packageUnit = packageEditorState?.unit.orEmpty(),
                isSaveEnabled = packageEditorState?.isSaveEnabled == true,
                isSubmitting = isSavingPackage,
                onPackageNameChange = { value ->
                    packageEditorState = packageEditorState?.copy(name = value)
                },
                onPackagePriceChange = { value ->
                    packageEditorState = packageEditorState?.copy(price = value)
                },
                onPackageDurationChange = { value ->
                    packageEditorState = packageEditorState?.copy(duration = value)
                },
                onPackageUnitChange = { value ->
                    packageEditorState = packageEditorState?.copy(unit = value)
                },
                onDismiss = {
                    if (!isSavingPackage) {
                        packageEditorState = null
                    }
                },
                onSave = {
                    val draft = packageEditorState ?: return@InventoryPackageEditorSheet
                    coroutineScope.launch {
                        if (draft.isEditMode) {
                            viewModel.updatePackage(
                                packageData = draft.toPackageData(),
                                onComplete = {
                                    packageEditorState = null
                                    scaffoldState.snackbarHostState.showQuickSnackbar(
                                        context.getString(
                                            R.string.inventory_package_update_success,
                                            draft.name.trim()
                                        )
                                    )
                                },
                                onError = { message ->
                                    scaffoldState.snackbarHostState.showQuickSnackbar(
                                        message.ifBlank {
                                            context.getString(R.string.inventory_package_update_failed)
                                        }
                                    )
                                }
                            )
                        } else {
                            viewModel.submitPackage(
                                packageData = draft.toPackageData(),
                                onComplete = {
                                    packageEditorState = null
                                    scaffoldState.snackbarHostState.showQuickSnackbar(
                                        context.getString(
                                            R.string.inventory_package_add_success,
                                            draft.name.trim()
                                        )
                                    )
                                },
                                onError = { message ->
                                    scaffoldState.snackbarHostState.showQuickSnackbar(
                                        message.ifBlank {
                                            context.getString(R.string.inventory_package_add_failed)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Composable
fun InventoryContent(
    state: InventoryUiState,
    bannerState: InlineAdaptiveBannerAdState,
    modifier: Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onAddPackage: () -> Unit = {},
    onPackageClick: (PackageItem) -> Unit = {},
    onSuggestedPackageClick: (String) -> Unit = {}
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            item {
                SectionOrLoading(
                    isLoading = state.packages.isLoading,
                    error = state.packages.errorMessage,
                    hasContent = !state.packages.data.isNullOrEmpty(),
                    showMiniLoading = false,
                    content = {
                        SetupPackageSection(
                            packages = state.packages.data.orEmpty(),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onAddPackage = onAddPackage,
                            onPackageClick = onPackageClick
                        )
                    }
                )
            }

            item(key = "inventory_inline_banner") {
                InlineAdaptiveBannerAd(state = bannerState)
            }

            item {
                SectionOrLoading(
                    isLoading = state.otherPackages.isLoading,
                    error = state.otherPackages.errorMessage,
                    hasContent = !state.otherPackages.data.isNullOrEmpty(),
                    showMiniLoading = false,
                    content = {
                        OtherPackagesSection(
                            data = state.otherPackages.data.orEmpty(),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onPackageSuggestionClick = onSuggestedPackageClick
                        )
                    }
                )
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OtherPackagesSection(
    data: List<String>,
    modifier: Modifier,
    onPackageSuggestionClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.inventory_unmapped_title),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.inventory_unmapped_description),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.appMutedContent
        )

        if (data.isEmpty()) {
            InventoryInfoCard(
                body = stringResource(R.string.inventory_unmapped_empty_body),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = stringResource(R.string.inventory_unmapped_tap_hint),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.appMutedContent
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.forEach { label ->
                    InventorySuggestionChip(
                        label = label,
                        onClick = { onPackageSuggestionClick(label) }
                    )
                }
            }
        }
    }
}

@Composable
fun SetupPackageSection(
    packages: List<PackageItem>,
    modifier: Modifier,
    onAddPackage: () -> Unit = {},
    onPackageClick: (PackageItem) -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.inventory_package_master_title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddPackage,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Text(text = stringResource(R.string.inventory_package_add))
            }
        }
        Text(
            text = stringResource(R.string.inventory_package_master_description),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.appMutedContent
        )

        if (packages.isEmpty()) {
            InventoryInfoCard(
                body = stringResource(R.string.inventory_package_empty_body),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                packages.forEach { item ->
                    InventoryPackageRow(
                        item = item,
                        onClick = { onPackageClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryPackageRow(
    item: PackageItem,
    onClick: () -> Unit = {}
) {
    Card(
        backgroundColor = MaterialTheme.colors.appCardSurface,
        shape = RoundedCornerShape(20.dp),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.appBorderSoft,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.displayRate,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.appMutedContent,
                        fontWeight = FontWeight.Medium
                    )

                    Box(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colors.appBorderSoft,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = packageDurationText(item),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.appMutedContent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colors.appMutedContent
            )
        }
    }
}

@Composable
private fun InventorySuggestionChip(
    label: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.appBorderSoft,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InventoryInfoCard(
    body: String,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = MaterialTheme.colors.appMutedContainer,
        shape = RoundedCornerShape(18.dp),
        elevation = 0.dp,
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.appBorderSoft,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Text(
            text = body,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.appMutedContent,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun packageDurationText(item: PackageItem): String {
    val duration = item.work.trim()
    return when {
        duration.equals("same day", ignoreCase = true) -> {
            stringResource(R.string.inventory_duration_same_day)
        }

        duration.endsWith("h", ignoreCase = true) -> {
            val totalHours = duration.dropLast(1).toIntOrNull()
            if (totalHours != null) {
                stringResource(R.string.inventory_duration_hours, totalHours)
            } else {
                duration
            }
        }

        duration.endsWith("d", ignoreCase = true) -> {
            val totalDays = duration.dropLast(1).toIntOrNull()
            if (totalDays != null) {
                stringResource(R.string.inventory_duration_days, totalDays)
            } else {
                duration
            }
        }

        duration.isNotBlank() -> duration
        else -> stringResource(R.string.inventory_duration_unavailable)
    }
}

private data class InventoryPackageEditorState(
    val sheetRowIndex: Int? = null,
    val name: String = "",
    val price: String = "",
    val duration: String = "",
    val unit: String = ""
) {
    val isEditMode: Boolean
        get() = sheetRowIndex != null

    val isSaveEnabled: Boolean
        get() = name.trim().isNotBlank() &&
            price.trim().isNotBlank() &&
            duration.trim().isNotBlank() &&
            unit.trim().isNotBlank()

    fun toPackageData(): PackageData {
        return PackageData(
            price = price.trim(),
            name = normalizeInventoryLabel(name),
            duration = duration.trim(),
            unit = unit.trim(),
            sheetRowIndex = sheetRowIndex ?: -1
        )
    }
}

private fun PackageItem.toEditorState(): InventoryPackageEditorState {
    return InventoryPackageEditorState(
        sheetRowIndex = sheetRowIndex,
        name = name,
        price = price.filter(Char::isDigit),
        duration = work,
        unit = unit
    )
}

private fun normalizeInventoryLabel(value: String): String {
    return value.trim().replace(Regex("\\s+"), " ")
}

@Composable
@Preview
fun PreviewInventoryScreen() {
    val bannerState = rememberInlineAdaptiveBannerAdState("preview_inventory_inline")
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { DefaultTopAppBar(stringResource(R.string.inventory_title)) },
        snackbarHost = { SnackbarHost(hostState = scaffoldState.snackbarHostState) }
    ) { padding ->
        InventoryContent(
            state = dummyInventoryUiState,
            bannerState = bannerState,
            modifier = Modifier.padding(padding)
        )
    }
}
