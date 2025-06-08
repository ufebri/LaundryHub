package com.raylabs.laundryhub.ui.inventory

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.ui.common.dummy.inventory.dummyInventoryUiState
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.inventory.state.InventoryCardItemData
import com.raylabs.laundryhub.ui.inventory.state.InventoryGroupItem
import com.raylabs.laundryhub.ui.inventory.state.InventoryUiState
import com.raylabs.laundryhub.ui.inventory.state.MachineItem
import com.raylabs.laundryhub.ui.inventory.state.PackageItem

@Composable
fun InventoryScreenView(
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    Scaffold(
        topBar = { DefaultTopAppBar("Inventory") }
    ) { padding ->
        InventoryContent(state, modifier = Modifier.padding(padding))
    }
}

@Composable
fun InventoryContent(state: InventoryUiState, modifier: Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        state.inventory.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
        state.packages.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SectionOrLoading(
                isLoading = state.inventory.isLoading,
                error = state.inventory.errorMessage,
                content = {
                    InventoryGrid(
                        data = state.inventory.data.orEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .heightIn(max = 400.dp)
                    )
                }
            )
        }

        item {
            SectionOrLoading(
                isLoading = state.packages.isLoading,
                error = state.packages.errorMessage,
                content = {
                    SetupPackageSection(
                        packages = state.packages.data.orEmpty(),
                        modifier = modifier.padding(horizontal = 16.dp)
                    )
                }
            )
        }

        item {
            SectionOrLoading(
                isLoading = state.otherPackages.isLoading,
                error = state.otherPackages.errorMessage,
                content = {
                    OtherPackagesSection(state.otherPackages.data.orEmpty(), modifier)
                }
            )
        }
    }
}

@Composable
fun OtherPackagesSection(data: List<String>, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Others Package",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "The Other Package maybe you can configure the price",
            style = MaterialTheme.typography.body2,
            color = Color.Gray
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp) // Batasi tinggi maksimal
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(data) { label ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
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
    onAddClicked: () -> Unit = {}
) {
    Column {
        // Header with title + add button
        Row(
            modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Setup Package",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clickable { onAddClicked() }
                    .padding(4.dp) // opsional untuk ruang klik
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Package",
                    tint = Color(0xFF5B3E9E) // match warna
                )
            }
        }

        // Card section
        Card(
            backgroundColor = Color(0xFF5B3E9E),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Package",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                packages.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = item.displayPrice,
                            color = Color.White,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun InventoryCardItem(
    title: String,
    subtitle: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(100.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color.White,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
fun InventoryGrid(
    data: List<InventoryGroupItem>,
    modifier: Modifier = Modifier
) {
    val items = data.map {
        InventoryCardItemData(
            title = it.stationType,
            subtitle = if (it.availableCount == 0) "All machines are in use"
            else "${it.availableCount} machine(s) available",
            color = Color(0xFF5B3E9E)
        )
    } + listOf(
        InventoryCardItemData(
            title = "Add Machine",
            subtitle = "Tap to Add Machine",
            color = Color(0xFFB3261E)
        )
    )

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(items) { item ->
            InventoryCardItem(
                title = item.title,
                subtitle = item.subtitle,
                backgroundColor = item.color
            )
        }
    }
}

@Composable
@Preview
fun PreviewInventoryScreen() {
    Scaffold(
        topBar = { DefaultTopAppBar("Inventory") }
    ) { padding ->
        InventoryContent(dummyInventoryUiState, modifier = Modifier.padding(padding))
    }
}