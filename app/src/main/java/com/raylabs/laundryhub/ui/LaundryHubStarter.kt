package com.raylabs.laundryhub.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.raylabs.laundryhub.core.di.GoogleSignInClientEntryPoint
import com.raylabs.laundryhub.ui.common.util.WhatsAppHelper
import com.raylabs.laundryhub.ui.history.HistoryScreenView
import com.raylabs.laundryhub.ui.home.HomeScreen
import com.raylabs.laundryhub.ui.home.HomeViewModel
import com.raylabs.laundryhub.ui.inventory.InventoryScreenView
import com.raylabs.laundryhub.ui.navigation.BottomNavItem
import com.raylabs.laundryhub.ui.onboarding.LoginViewModel
import com.raylabs.laundryhub.ui.onboarding.OnboardingScreen
import com.raylabs.laundryhub.ui.onboarding.state.getListOnboardingPage
import com.raylabs.laundryhub.ui.order.OrderBottomSheet
import com.raylabs.laundryhub.ui.order.OrderViewModel
import com.raylabs.laundryhub.ui.order.state.toOrderData
import com.raylabs.laundryhub.ui.profile.ProfileScreenView
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun AppRoot(
    loginViewModel: LoginViewModel = hiltViewModel(),
    googleSignInClient: GoogleSignInClient =
        EntryPointAccessors.fromApplication(
            LocalContext.current.applicationContext,
            GoogleSignInClientEntryPoint::class.java
        ).googleSignInClient()
) {
    val user by loginViewModel.userState.collectAsState()
    val isLoading by loginViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            val idToken = account?.idToken
            if (!idToken.isNullOrEmpty()) {
                loginViewModel.signInGoogle(idToken)
            }
        } catch (_: Exception) {
            // handle error
            Toast.makeText(context, "Failed to sign in", Toast.LENGTH_SHORT).show()
        }
    }

    when {
        isLoading -> { // Show loading indicator
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        user != null -> {
            LaundryHubStarter(loginViewModel = loginViewModel)
        }

        else -> {
            // Show onboarding screen
            OnboardingScreen(
                pages = getListOnboardingPage,
                onLoginClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LaundryHubStarter(
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel,
    navController: NavHostController = rememberNavController()
) {
    val orderViewModel: OrderViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val showNewOrderSheet = remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    val triggerOpenSheet = remember { mutableStateOf(false) }
    val showEditOrderSheet = remember { mutableStateOf(false) }

    fun dismissSheet() {
        scope.launch {
            scaffoldState.bottomSheetState.collapse()
            showNewOrderSheet.value = false
            showEditOrderSheet.value = false
            orderViewModel.resetForm()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            when {
                showNewOrderSheet.value -> ShowOrderBottomSheet(
                    orderViewModel,
                    homeViewModel,
                    scope,
                    snackBarHostState
                ) { dismissSheet() }

                showEditOrderSheet.value -> ShowOrderBottomSheet(
                    orderViewModel,
                    homeViewModel,
                    scope,
                    snackBarHostState
                ) { dismissSheet() }

                else -> {
                    // Tetap render konten kosong agar sheet tidak null
                    Spacer(Modifier.height(1.dp))
                }
            }
        }) {

        LaunchedEffect(triggerOpenSheet.value) {
            if (triggerOpenSheet.value) {
                delay(50)
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
                    showEditOrderSheet.value = false // Pastikan sheet edit tidak aktif
                    orderViewModel.resetForm() // Set mode new order
                    showNewOrderSheet.value = true
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
                            orderViewModel.resetForm()
                            orderViewModel.onOrderEditClick(orderId) {
                                showEditOrderSheet.value = true
                                triggerOpenSheet.value = true
                            }
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
                    ProfileScreenView(loginViewModel = loginViewModel)
                }
            }
        }
    }
}

@Composable
fun ShowOrderBottomSheet(
    orderViewModel: OrderViewModel,
    homeViewModel: HomeViewModel,
    scope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    dismissSheet: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by orderViewModel.uiState.collectAsState()

    Log.d(
        "onOrderCardClick",
        "DEBUG: Render Sheet — isEditMode=${uiState.isEditMode}, selectedPackage=${uiState.selectedPackage?.name}"
    )
    OrderBottomSheet(
        state = uiState,
        onNameChanged = { orderViewModel.updateField("name", it) },
        onPhoneChanged = { orderViewModel.onPhoneChanged(it) },
        onPriceChanged = { orderViewModel.onPriceChanged(it) },
        onPackageSelected = { orderViewModel.onPackageSelected(it) },
        onPaymentMethodSelected = { orderViewModel.updateField("paymentMethod", it) },
        onNoteChanged = { orderViewModel.updateField("note", it) },
        onSubmit = {
            uiState.lastOrderId?.let { id ->
                scope.launch {
                    orderViewModel.submitOrder(uiState.toOrderData(id), onComplete = {
                        homeViewModel.fetchOrder()
                        homeViewModel.fetchTodayIncome()
                        homeViewModel.fetchSummary()
                        delay(500)
                        dismissSheet()
                        val message = WhatsAppHelper.buildOrderMessage(
                            customerName = uiState.name,
                            packageName = uiState.selectedPackage?.name.orEmpty(),
                            total = uiState.price,
                            paymentStatus = uiState.paymentMethod
                        )
                        val phone = uiState.phone
                        orderViewModel.resetForm()
                        snackBarHostState.showSnackbar("Order #$id successfully submitted!, waiting for open wa...")
                        WhatsAppHelper.sendWhatsApp(context, phone, message)
                    })
                }
            }
        },
        onUpdate = {
            scope.launch {
                orderViewModel.updateOrder(uiState.toOrderData(uiState.orderID), onComplete = {
                    homeViewModel.fetchOrder()
                    homeViewModel.fetchTodayIncome()
                    homeViewModel.fetchSummary()
                    delay(500)
                    dismissSheet()
                    snackBarHostState.showSnackbar("Order #${uiState.orderID} successfully updated!")
                })
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
    LaundryHubStarter(loginViewModel = hiltViewModel<LoginViewModel>())
}