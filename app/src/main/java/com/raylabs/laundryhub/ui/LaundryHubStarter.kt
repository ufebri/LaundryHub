package com.raylabs.laundryhub.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
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
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.history.HistoryScreenView
import com.raylabs.laundryhub.ui.home.HomeScreen
import com.raylabs.laundryhub.ui.home.HomeViewModel
import com.raylabs.laundryhub.ui.inventory.InventoryScreenView
import com.raylabs.laundryhub.ui.navigation.BottomNavItem
import com.raylabs.laundryhub.ui.order.OrderBottomSheet
import com.raylabs.laundryhub.ui.order.OrderViewModel
import com.raylabs.laundryhub.ui.order.state.toHistoryData
import com.raylabs.laundryhub.ui.order.state.toOrderData
import com.raylabs.laundryhub.ui.profile.ProfileScreenView
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
    val state = orderViewModel.uiState
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val showOrderSheet = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val triggerOpenSheet = remember { mutableStateOf(false) }

    fun dismissSheet() {
        scope.launch {
            scaffoldState.bottomSheetState.collapse()
            showOrderSheet.value = false
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            if (showOrderSheet.value) {
                OrderBottomSheet(
                    state = state,
                    onNameChanged = { orderViewModel.updateField("name", it) },
                    onPhoneChanged = { orderViewModel.onPhoneChanged(it) },
                    onPriceChanged = { orderViewModel.onPriceChanged(it) },
                    onPackageSelected = { orderViewModel.onPackageSelected(it) },
                    onPaymentMethodSelected = { orderViewModel.updateField("paymentMethod", it) },
                    onNoteChanged = { orderViewModel.updateField("note", it) },
                    onSubmit = {
                        state.lastOrderId?.let { id ->
                            scope.launch {
                                orderViewModel.submitOrder(state.toOrderData(id), onComplete = {
                                    // Submit history after order is successfully submitted
                                    orderViewModel.submitHistory(state.toHistoryData(id))

                                    // Refresh home view model data
                                    homeViewModel.fetchOrder()
                                    // Dismiss the sheet and reset form
                                    dismissSheet()
                                    orderViewModel.resetForm()
                                    snackbarHostState.showSnackbar("Order #$id successfully submitted!")
                                })
                            }
                        }
                    }
                )
            } else {
                // Tetap render konten kosong agar sheet tidak null
                Spacer(Modifier.height(1.dp))
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
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(top = 48.dp)
                )
            },
            bottomBar = {
                BottomBar(navController, onOrderClick = {
                    showOrderSheet.value = true
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
                    HomeScreen(viewModel = homeViewModel)
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
fun BottomBar(
    navController: NavHostController,
    onOrderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomNavigation(
        modifier = modifier,
        backgroundColor = colorResource(id = R.color.colorPrimary),
        contentColor = Color.White
    ) {
        //List Menu Item
        val items = listOf(
            BottomNavItem.Home,
            BottomNavItem.History,
            BottomNavItem.Order,
            BottomNavItem.Inventory,
            BottomNavItem.Profile
        )
        BottomNavigation {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            items.map { item ->
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
}

@Preview(apiLevel = 33)
@Composable
fun BottomNavigationPreview() {
    LaundryHubStarter()
}