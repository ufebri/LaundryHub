package com.raylabs.laundryhub.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.raylabs.laundryhub.R
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
import com.raylabs.laundryhub.ui.order.state.OrderSheetUiState
import com.raylabs.laundryhub.ui.order.state.dismissSheet
import com.raylabs.laundryhub.ui.order.state.openEditSheet
import com.raylabs.laundryhub.ui.order.state.openNewSheet
import com.raylabs.laundryhub.ui.order.state.toOrderData
import com.raylabs.laundryhub.ui.outcome.OutcomeScreen
import com.raylabs.laundryhub.ui.profile.ProfileScreenView
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.MaterialTheme as Material2Theme


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
            Toast.makeText(
                context,
                context.getString(R.string.failed_sign_in_message),
                Toast.LENGTH_SHORT
            ).show()
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaundryHubStarter(
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel,
    navController: NavHostController = rememberNavController()
) {
    val orderViewModel: OrderViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    var orderSheetUiState by remember { mutableStateOf(OrderSheetUiState()) }
    val snackBarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current
    val colors = Material2Theme.colors
    val scaffoldColor = colors.background
    val surfaceColor = colors.background
    val sheetColor = colors.surface

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.statusBarColor = scaffoldColor.toArgb()
            window?.let {
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
            }
        }
    }

    fun dismissSheet() {
        scope.launch {
            sheetState.hide()
            orderSheetUiState = orderSheetUiState.dismissSheet()
            orderViewModel.resetForm()
        }
    }

    fun openNewSheet() {
        orderSheetUiState = orderSheetUiState.openNewSheet()
        scope.launch { sheetState.show() }
    }

    fun openEditSheet() {
        orderSheetUiState = orderSheetUiState.openEditSheet()
        scope.launch { sheetState.show() }
    }

    if (orderSheetUiState.isSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { dismissSheet() },
            sheetState = sheetState,
            containerColor = sheetColor,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            ShowOrderBottomSheet(
                orderViewModel,
                homeViewModel,
                scope,
                snackBarHostState
            ) { dismissSheet() }
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
                orderViewModel.resetForm() // Set mode new order
                openNewSheet()
            })
        },
        containerColor = scaffoldColor,
        modifier = modifier
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            color = surfaceColor
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.screenRoute,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavItem.Home.screenRoute) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onOrderCardClick = { orderId ->
                            orderViewModel.resetForm()
                            orderViewModel.onOrderEditClick(orderId) {
                                openEditSheet()
                            }
                        },
                        onTodayActivityClick = { activityId ->
                            orderViewModel.resetForm()
                            orderViewModel.onOrderEditClick(activityId) {
                                openEditSheet()
                            }
                        }
                    )
                }
                composable(BottomNavItem.History.screenRoute) {
                    HistoryScreenView()
                }
                composable(BottomNavItem.Outcome.screenRoute) {
                    OutcomeScreen()
                }
                composable(BottomNavItem.Profile.screenRoute) {
                    ProfileScreenView(
                        loginViewModel = loginViewModel,
                        onInventoryClick = {
                            navController.navigate(BottomNavItem.Inventory.screenRoute)
                        }
                    )
                }
                composable(BottomNavItem.Inventory.screenRoute) {
                    InventoryScreenView()
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

    OrderBottomSheet(
        state = uiState,
        onNameChanged = { orderViewModel.updateField("name", it) },
        onPhoneChanged = { orderViewModel.onPhoneChanged(it) },
        onPriceChanged = { orderViewModel.onPriceChanged(it) },
        onPackageSelected = { orderViewModel.onPackageSelected(it) },
        onPaymentMethodSelected = { orderViewModel.updateField("paymentMethod", it) },
        onNoteChanged = { orderViewModel.updateField("note", it) },
        onOrderDateSelected = { orderViewModel.onOrderDateSelected(it) },
        onSubmit = {
            uiState.lastOrderId?.let { id ->
                scope.launch {
                    orderViewModel.submitOrder(uiState.toOrderData(id), onComplete = {
                        homeViewModel.fetchOrder()
                        homeViewModel.fetchTodayIncome()
                        homeViewModel.fetchSummary()
                        delay(500)
                        dismissSheet()

                        val phone = uiState.phone
                        if (phone.isNotEmpty()) {
                            val message = WhatsAppHelper.buildOrderMessage(
                                customerName = uiState.name,
                                packageName = uiState.selectedPackage?.name.orEmpty(),
                                total = uiState.price,
                                paymentStatus = uiState.paymentMethod
                            )
                            WhatsAppHelper.sendWhatsApp(context, phone, message)
                        }

                        orderViewModel.resetForm()
                        snackBarHostState.showSnackbar("Order #$id successfully submitted!, waiting for open wa...")
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
    val colors = Material2Theme.colors
    val unselectedColor = colors.onSurface.copy(alpha = 0.6f)
    NavigationBar(
        modifier = modifier,
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            BottomNavItem.Home,
            BottomNavItem.History,
            BottomNavItem.Order,
            BottomNavItem.Outcome,
            BottomNavItem.Profile
        )
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
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
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSurface,
                    selectedTextColor = colors.onSurface,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor,
                    indicatorColor = Color.Transparent
                ),
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
