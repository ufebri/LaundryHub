package com.raylabs.laundryhub.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.data.service.GoogleCredentialAuthManager
import com.raylabs.laundryhub.core.di.GoogleAuthEntryPoint
import com.raylabs.laundryhub.core.reminder.ReminderNotificationConfig
import com.raylabs.laundryhub.ui.common.navigation.BottomNavItem
import com.raylabs.laundryhub.ui.common.util.WhatsAppHelper
import com.raylabs.laundryhub.ui.common.util.showQuickSnackbar
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.history.HistoryScreenView
import com.raylabs.laundryhub.ui.home.GrossDetailScreenView
import com.raylabs.laundryhub.ui.home.HomeScreen
import com.raylabs.laundryhub.ui.home.HomeViewModel
import com.raylabs.laundryhub.ui.onboarding.LoginViewModel
import com.raylabs.laundryhub.ui.onboarding.OnboardingScreen
import com.raylabs.laundryhub.ui.onboarding.state.getListOnboardingPage
import com.raylabs.laundryhub.ui.order.OrderBottomSheet
import com.raylabs.laundryhub.ui.order.OrderViewModel
import com.raylabs.laundryhub.ui.order.state.toOrderData
import com.raylabs.laundryhub.ui.outcome.OutcomeScreenView
import com.raylabs.laundryhub.ui.profile.ProfileScreenView
import com.raylabs.laundryhub.ui.profile.inventory.InventoryScreenView
import com.raylabs.laundryhub.ui.reminder.ReminderInboxScreen
import com.raylabs.laundryhub.ui.reminder.ReminderIntroScreen
import com.raylabs.laundryhub.ui.theme.modalSheetTop
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val INVENTORY_ROUTE = "inventory"
private const val GROSS_ROUTE = "gross"
private const val REMINDER_INTRO_ROUTE = "reminder_intro"
private const val REMINDER_INBOX_ROUTE = "reminder_inbox"

@Composable
fun AppRoot(
    loginViewModel: LoginViewModel = hiltViewModel(),
    googleCredentialAuthManager: GoogleCredentialAuthManager =
        EntryPointAccessors.fromApplication(
            LocalContext.current.applicationContext,
            GoogleAuthEntryPoint::class.java
        ).googleCredentialAuthManager(),
    notificationDestination: String? = null,
    onNotificationDestinationHandled: () -> Unit = {}
) {
    val user by loginViewModel.userState.collectAsState()
    val isLoading by loginViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    when {
        isLoading -> {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        user == null -> {
            // Show onboarding screen
            OnboardingScreen(
                pages = getListOnboardingPage,
                onLoginClick = {
                    if (activity == null) {
                        return@OnboardingScreen
                    }

                    scope.launch {
                        runCatching {
                            googleCredentialAuthManager.signIn(activity)
                        }.onSuccess { result ->
                            loginViewModel.signInGoogle(result.idToken)
                        }.onFailure { throwable ->
                            Log.e("AppRootAuth", "Google sign-in failed: ${throwable.message}", throwable)
                        }
                    }
                }
            )
        }

        user != null -> {
            LaundryHubStarter(
                loginViewModel = loginViewModel,
                notificationDestination = notificationDestination,
                onNotificationDestinationHandled = onNotificationDestinationHandled
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LaundryHubStarter(
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel,
    navController: NavHostController = rememberNavController(),
    notificationDestination: String? = null,
    onNotificationDestinationHandled: () -> Unit = {}
) {
    val orderViewModel: OrderViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val showNewOrderSheet = remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    val triggerOpenSheet = remember { mutableStateOf(false) }
    val showEditOrderSheet = remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarRoutes = remember {
        setOf(
            BottomNavItem.Home.screenRoute,
            BottomNavItem.History.screenRoute,
            BottomNavItem.Order.screenRoute,
            BottomNavItem.Outcome.screenRoute,
            BottomNavItem.Profile.screenRoute
        )
    }
    val homeBannerState = rememberInlineAdaptiveBannerAdState("home_inline")
    val historyBannerState = rememberInlineAdaptiveBannerAdState("history_inline")
    val outcomeBannerState = rememberInlineAdaptiveBannerAdState("outcome_inline")
    val profileBannerState = rememberInlineAdaptiveBannerAdState("profile_inline")

    fun dismissSheet() {
        scope.launch {
            scaffoldState.bottomSheetState.collapse()
            showNewOrderSheet.value = false
            showEditOrderSheet.value = false
            orderViewModel.resetForm()
        }
    }

    fun openReminderOrder(orderId: String) {
        orderViewModel.resetForm()
        orderViewModel.onOrderEditClick(orderId) {
            showNewOrderSheet.value = false
            showEditOrderSheet.value = true
            triggerOpenSheet.value = true
        }
    }

    LaunchedEffect(notificationDestination, currentRoute) {
        when (notificationDestination) {
            ReminderNotificationConfig.DESTINATION_REMINDER_INBOX -> {
                if (currentRoute != REMINDER_INBOX_ROUTE) {
                    navController.navigate(REMINDER_INBOX_ROUTE)
                }
                onNotificationDestinationHandled()
            }

            null -> Unit
            else -> onNotificationDestinationHandled()
        }
    }

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetShape = MaterialTheme.shapes.modalSheetTop,
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
                if (currentRoute in bottomBarRoutes) {
                    BottomBar(navController, onOrderClick = {
                        showEditOrderSheet.value = false
                        orderViewModel.resetForm()
                        showNewOrderSheet.value = true
                        triggerOpenSheet.value = true
                    })
                }
            },
            modifier = Modifier
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.screenRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Home.screenRoute) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        bannerState = homeBannerState,
                        onOrderCardClick = { orderId ->
                            orderViewModel.resetForm()
                            orderViewModel.onOrderEditClick(orderId) {
                                showEditOrderSheet.value = true
                                triggerOpenSheet.value = true
                            }
                        },
                        onTodayActivityClick = { activityId ->
                            orderViewModel.resetForm()
                            orderViewModel.onOrderEditClick(activityId) {
                                showEditOrderSheet.value = true
                                triggerOpenSheet.value = true
                            }
                        },
                        onGrossCardClick = {
                            navController.navigate(GROSS_ROUTE)
                        },
                        onReminderDiscoveryClick = { isReminderEnabled ->
                            navController.navigate(
                                if (isReminderEnabled) REMINDER_INBOX_ROUTE else REMINDER_INTRO_ROUTE
                            ) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(BottomNavItem.History.screenRoute) {
                    HistoryScreenView(
                        bannerState = historyBannerState,
                        onEditOrderRequest = { orderId ->
                            orderViewModel.resetForm()
                            orderViewModel.onOrderEditClick(orderId) {
                                showEditOrderSheet.value = true
                                triggerOpenSheet.value = true
                            }
                        },
                        onOrderChanged = {
                            scope.launchOrderChangedRefresh(homeViewModel)
                        }
                    )
                }
                composable(BottomNavItem.Outcome.screenRoute) {
                    OutcomeScreenView(
                        bannerState = outcomeBannerState,
                        onOutcomeChanged = {
                            scope.launchOutcomeChangedRefresh(homeViewModel)
                        }
                    )
                }
                composable(BottomNavItem.Profile.screenRoute) {
                    ProfileScreenView(
                        loginViewModel = loginViewModel,
                        bannerState = profileBannerState,
                        onInventoryClick = { navController.navigate(INVENTORY_ROUTE) },
                        onReminderSettingsClick = {
                            navController.navigate(REMINDER_INTRO_ROUTE) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(INVENTORY_ROUTE) {
                    InventoryScreenView(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(GROSS_ROUTE) {
                    val pagingItems = homeViewModel.grossPagingData.collectAsLazyPagingItems()
                    GrossDetailScreenView(
                        pagingItems = pagingItems,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(REMINDER_INTRO_ROUTE) {
                    ReminderIntroScreen(
                        onBack = { navController.popBackStack() },
                        onOpenInbox = {
                            navController.navigate(REMINDER_INBOX_ROUTE) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(REMINDER_INBOX_ROUTE) {
                    ReminderInboxScreen(
                        onBack = { navController.popBackStack() },
                        onOpenOrder = ::openReminderOrder
                    )
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
    val orderIdUnavailableMessage = stringResource(R.string.order_id_unavailable)
    val submitFailedMessage = stringResource(R.string.order_submit_failed)
    val updateFailedMessage = stringResource(R.string.order_update_failed)

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
            scope.launch {
                val currentState = orderViewModel.uiState.value
                val orderId = currentState.lastOrderId
                    ?: orderViewModel.resolveLastOrderIdForSubmit()

                if (orderId.isNullOrBlank()) {
                    val errorMessage = orderViewModel.uiState.value.lastOrderIdError
                        ?: orderIdUnavailableMessage
                    snackBarHostState.showQuickSnackbar(errorMessage)
                    return@launch
                }

                orderViewModel.submitOrder(
                    orderViewModel.uiState.value.toOrderData(orderId),
                    onComplete = {
                        val submittedState = orderViewModel.uiState.value
                        scope.launchOrderChangedRefresh(homeViewModel)
                        dismissSheet()

                        val phone = submittedState.phone
                        if (phone.isNotEmpty()) {
                            val message = WhatsAppHelper.buildOrderMessage(
                                customerName = submittedState.name,
                                packageName = submittedState.selectedPackage?.name.orEmpty(),
                                total = submittedState.price,
                                paymentStatus = submittedState.paymentMethod
                            )
                            WhatsAppHelper.sendWhatsApp(context, phone, message)
                        }

                        val successMessage = if (phone.isNotEmpty()) {
                            context.getString(R.string.order_submit_success_opening_whatsapp, orderId)
                        } else {
                            context.getString(R.string.order_submit_success, orderId)
                        }
                        snackBarHostState.showQuickSnackbar(successMessage)
                    },
                    onError = { errorMessage ->
                        snackBarHostState.showQuickSnackbar(errorMessage.ifBlank { submitFailedMessage })
                    }
                )
            }
        },
        onUpdate = {
            scope.launch {
                orderViewModel.updateOrder(
                    uiState.toOrderData(uiState.orderID),
                    onComplete = {
                        scope.launchOrderChangedRefresh(homeViewModel)
                        dismissSheet()
                        snackBarHostState.showQuickSnackbar(
                            context.getString(R.string.order_update_success, uiState.orderID)
                        )
                    },
                    onError = { errorMessage ->
                        snackBarHostState.showQuickSnackbar(errorMessage.ifBlank { updateFailedMessage })
                    }
                )
            }
        }
    )
}

private fun CoroutineScope.launchOrderChangedRefresh(homeViewModel: HomeViewModel) {
    launch {
        runCatching { homeViewModel.refreshAfterOrderChanged() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Log.w("LaundryHubStarter", "Home refresh after order change failed", error)
            }
    }
}

private fun CoroutineScope.launchOutcomeChangedRefresh(homeViewModel: HomeViewModel) {
    launch {
        runCatching { homeViewModel.refreshAfterOutcomeChanged() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Log.w("LaundryHubStarter", "Home refresh after outcome change failed", error)
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
        modifier = modifier.background(Color.White),
        backgroundColor = Color.White,
        contentColor = Color.Black,
        elevation = 0.dp
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
            BottomNavigationItem(
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = stringResource(item.title)
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.title),
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
