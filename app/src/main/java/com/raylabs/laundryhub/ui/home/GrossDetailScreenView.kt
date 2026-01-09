package com.raylabs.laundryhub.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.TextUtil.capitalizeFirstLetter
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.home.state.GrossItem
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme

@Composable
fun GrossDetailScreenView(
    grossState: SectionState<List<GrossItem>>,
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
            val previousColor = window.statusBarColor
            val previousAppearance = controller.isAppearanceLightStatusBars
            window.statusBarColor = Color.Transparent.toArgb()
            controller.isAppearanceLightStatusBars = useDarkStatusIcons
            onDispose {
                window.statusBarColor = previousColor
                controller.isAppearanceLightStatusBars = previousAppearance
            }
        }
    }

    Scaffold(
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
            grossState = grossState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
fun GrossDetailContent(
    grossState: SectionState<List<GrossItem>>,
    modifier: Modifier = Modifier
) {
    SectionOrLoading(
        isLoading = grossState.isLoading,
        error = grossState.errorMessage,
        content = {
            val items = grossState.data.orEmpty()
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.gross_income_empty),
                    style = MaterialTheme.typography.body2,
                    modifier = modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = modifier,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { item ->
                        GrossItemCard(item = item)
                    }
                }
            }
        }
    )
}

@Composable
private fun GrossItemCard(item: GrossItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color(0xFFFAFDFB),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.month.capitalizeFirstLetter(),
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = item.totalNominal,
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
                    value = item.orderCount
                )
                Spacer(modifier = Modifier.weight(1f))
                GrossChip(
                    label = stringResource(R.string.gross_tax),
                    value = item.tax
                )
            }
        }
    }
}

@Composable
private fun GrossChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(Color(0xFFE6F4EA), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
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
fun PreviewGrossDetailContent() {
    GrossDetailContent(
        grossState = SectionState(
            data = listOf(
                GrossItem("november", "Rp3.563.000", "149", "Rp17.815"),
                GrossItem("desember", "Rp3.944.000", "158", "Rp19.720")
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewGrossDetailScreen() {
    LaundryHubTheme {
        GrossDetailScreenView(
            grossState = SectionState(
                data = listOf(
                    GrossItem("maret", "Rp1.038.150", "35", "Rp5.191"),
                    GrossItem("april", "Rp2.374.200", "77", "Rp11.871")
                )
            ),
            onBack = {}
        )
    }
}
