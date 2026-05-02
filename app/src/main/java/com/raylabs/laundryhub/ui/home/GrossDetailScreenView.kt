package com.raylabs.laundryhub.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.paging.compose.LazyPagingItems
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.ui.home.state.toUi
import com.raylabs.laundryhub.ui.common.util.TextUtil.capitalizeFirstLetter
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAd
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.home.state.toUI
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import com.raylabs.laundryhub.ui.theme.appCardSurface
import com.raylabs.laundryhub.ui.theme.appMutedInfoContainer
import com.raylabs.laundryhub.ui.theme.appMutedInfoContent

@Composable
fun GrossDetailScreenView(
    pagingItems: LazyPagingItems<GrossData>,
    onBack: () -> Unit
) {
    val surfaceColor = MaterialTheme.colors.surface
    val useDarkStatusIcons = surfaceColor.luminance() > 0.5f
    val isPreview = LocalInspectionMode.current
    if (!isPreview) {
        val view = LocalView.current
        DisposableEffect(view, surfaceColor, useDarkStatusIcons) {
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

    val bannerState = rememberInlineAdaptiveBannerAdState("gross_detail_inline")

    Scaffold(
        backgroundColor = MaterialTheme.colors.background,
        topBar = {
            Column {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor)
                        .statusBarsPadding()
                )
                TopAppBar(
                    title = { Text(text = stringResource(R.string.gross_income)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    backgroundColor = surfaceColor,
                    elevation = 0.dp
                )
            }
        }
    ) { padding ->
        GrossDetailContent(
            pagingItems = pagingItems,
            bannerState = bannerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
fun GrossDetailContent(
    pagingItems: LazyPagingItems<GrossData>,
    bannerState: InlineAdaptiveBannerAdState,
    modifier: Modifier = Modifier
) {
    val isRefreshing = pagingItems.loadState.refresh is androidx.paging.LoadState.Loading
    
    Box(modifier = modifier.fillMaxSize()) {
        if (pagingItems.itemCount == 0 && !isRefreshing) {
            Text(
                text = stringResource(R.string.gross_income_empty),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(16.dp).align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "gross_detail_inline_banner") {
                    InlineAdaptiveBannerAd(
                        state = bannerState,
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
                    )
                }

                items(
                    count = pagingItems.itemCount,
                    key = { index -> pagingItems[index]?.month ?: "placeholder_\$index" }
                ) { index ->
                    val data = pagingItems[index]
                    if (data != null) {
                        val item = data.toUi()
                        GrossItemCard(
                            month = item.month,
                            totalNominal = item.totalNominal,
                            orderCount = item.orderCount,
                            tax = item.tax
                        )
                    }
                }
            }
        }
        
        if (isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun GrossItemCard(
    month: String,
    totalNominal: String,
    orderCount: String,
    tax: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.appCardSurface,
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = month.capitalizeFirstLetter(),
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = totalNominal,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GrossChip(
                    label = stringResource(R.string.gross_orders),
                    value = orderCount
                )
                Spacer(modifier = Modifier.weight(1f))
                GrossChip(
                    label = stringResource(R.string.gross_tax),
                    value = tax
                )
            }
        }
    }
}

@Composable
private fun GrossChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.appMutedInfoContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.appMutedInfoContent
        )
        Text(
            text = value,
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGrossDetailScreen() {
    Text("Gross Detail Preview")
}
