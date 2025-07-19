package com.raylabs.laundryhub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raylabs.laundryhub.ui.history.HistoryScreenView
import com.raylabs.laundryhub.ui.home.HomeScreen
import com.raylabs.laundryhub.ui.home.HomeViewModel
import com.raylabs.laundryhub.ui.inventory.InventoryScreenView
import com.raylabs.laundryhub.ui.navigation.BottomNavItem
import com.raylabs.laundryhub.ui.order.OrderBottomSheet
import com.raylabs.laundryhub.ui.order.OrderViewModel
import com.raylabs.laundryhub.ui.order.state.toOrderData
import com.raylabs.laundryhub.ui.profile.ProfileScreenView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LaundryHubStarter(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val orderViewModel: OrderViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val showOrderSheet = remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    val triggerOpenSheet = remember { mutableStateOf(false) }
    val showDetailOrderSheet = remember { mutableStateOf(false) }

    fun dismissSheet() {
        scope.launch {
            scaffoldState.bottomSheetState.collapse()
            showOrderSheet.value = false
            showDetailOrderSheet.value = false
            orderViewModel.resetForm()
            homeViewModel.clearSelectedOrder()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            when {
                showOrderSheet.value -> {
                    ShowOrderBottomSheet(
                        orderViewModel,
                        homeViewModel,
                        scope,
                        snackBarHostState
                    ) { dismissSheet() }
                }

                showDetailOrderSheet.value -> {
                    ShowDetailOrderBottomSheet(homeViewModel)
                }

                else -> {
                    // Tetap render konten kosong agar sheet tidak null
                    Spacer(Modifier.height(1.dp))
                }
            }
        }) {

        LaunchedEffect(triggerOpenSheet.value) {
            if (triggerOpenSheet.value) {
                scaffoldState.bottomSheetState.expand()
                triggerOpenSheet.value = false
            }
        }


        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackBarHostState,
                    modifier = Modifier.padding(top = 48.dp)
                )
            },
            bottomBar = {
                BottomBar(navController, onOrderClick = {
                    showOrderSheet.value = true
                    showDetailOrderSheet.value = false // Pastikan hanya satu sheet aktif
                    triggerOpenSheet.value = true
                })
            },
            modifier = modifier
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.screenRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Home.screenRoute) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onOrderCardClick = { orderId ->
                            homeViewModel.setSelectedOrderId(orderId)
                            homeViewModel.getOrderById(orderId)
                            showDetailOrderSheet.value = true
                            showOrderSheet.value = false // Pastikan hanya satu sheet aktif
                            triggerOpenSheet.value = true
                        }
                    )
                }
                composable(BottomNavItem.History.screenRoute) {
                    HistoryScreenView()
                }
                composable(BottomNavItem.Inventory.screenRoute) {
                    InventoryScreenView()
                }
                composable(BottomNavItem.Profile.screenRoute) {
                    ProfileScreenView()
                }
            }
        }
    }
}

@Composable
fun ShowDetailOrderBottomSheet(homeViewModel: HomeViewModel) {
    val uiState by homeViewModel.uiState.collectAsState()
    uiState.historyOrder.data?.let { } ?: Spacer(modifier = Modifier.height(1.dp))
}

@Composable
fun ShowOrderBottomSheet(
    state: OrderViewModel,
    homeViewModel: HomeViewModel,
    scope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    dismissSheet: () -> Unit
) {
    OrderBottomSheet(
        state = state.uiState,
        onNameChanged = { state.updateField("name", it) },
        onPhoneChanged = { state.onPhoneChanged(it) },
        onPriceChanged = { state.onPriceChanged(it) },
        onPackageSelected = { state.onPackageSelected(it) },
        onPaymentMethodSelected = { state.updateField("paymentMethod", it) },
        onNoteChanged = { state.updateField("note", it) },
        onSubmit = {
            state.uiState.lastOrderId?.let { id ->
                scope.launch {
                    state.submitOrder(state.uiState.toOrderData(id), onComplete = {
                        // Refresh home view model data
                        homeViewModel.fetchOrder()

                        // Refresh income data
                        homeViewModel.fetchTodayIncome()

                        // Refresh summary data
                        homeViewModel.fetchSummary()

                        // Delay to ensure data is refreshed before dismissing
                        delay(500)

                        // Dismiss the sheet and reset form
                        dismissSheet()
                        state.resetForm()
                        snackBarHostState.showSnackbar("Order #$id successfully submitted!")
                    })
                }
            }
        }
    )
}

@Composable
fun BottomBar(
    navController: NavHostController,
    onOrderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomNavigation(
        modifier = modifier.background(Color.White),
        backgroundColor = Color.White,
        contentColor = Color.Black,
        elevation = 0.dp
    ) {
        val items = listOf(
            BottomNavItem.Home,
            BottomNavItem.History,
            BottomNavItem.Order,
            BottomNavItem.Inventory,
            BottomNavItem.Profile
        )
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        painterResource(id = item.icon),
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 9.sp
                    )
                },
                selectedContentColor = Color.Black,
                unselectedContentColor = Color.Black.copy(0.4f),
                alwaysShowLabel = true,
                selected = currentRoute == item.screenRoute,
                onClick = {
                    if (item.screenRoute == BottomNavItem.Order.screenRoute) {
                        onOrderClick()
                    } else {
                        navController.navigate(item.screenRoute) {
                            navController.graph.startDestinationRoute?.let { screenRoute ->
                                popUpTo(screenRoute) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Preview(apiLevel = 33)
@Composable
fun BottomNavigationPreview() {
    LaundryHubStarter()
}