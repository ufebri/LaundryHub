package com.raylabs.laundryhub.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.data.service.GoogleCredentialAuthManager
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.data.service.SpreadsheetIdParser
import com.raylabs.laundryhub.core.di.GoogleAuthEntryPoint
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import com.raylabs.laundryhub.core.reminder.ReminderNotificationConfig
import com.raylabs.laundryhub.ui.common.navigation.BottomNavItem
import com.raylabs.laundryhub.ui.common.util.WhatsAppHelper
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
import com.raylabs.laundryhub.ui.spreadsheet.SpreadsheetSetupScreen
import com.raylabs.laundryhub.ui.spreadsheet.SpreadsheetSetupViewModel
import com.raylabs.laundryhub.ui.theme.modalSheetTop
import dagger.hilt.android.EntryPointAccessors
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
    spreadsheetSetupViewModel: SpreadsheetSetupViewModel = hiltViewModel(),
    googleCredentialAuthManager: GoogleCredentialAuthManager =
        EntryPointAccessors.fromApplication(
            LocalContext.current.applicationContext,
            GoogleAuthEntryPoint::class.java
        ).googleCredentialAuthManager(),
    googleSheetsAuthorizationManager: GoogleSheetsAuthorizationManager =
        EntryPointAccessors.fromApplication(
            LocalContext.current.applicationContext,
            GoogleAuthEntryPoint::class.java
        ).googleSheetsAuthorizationManager()
    ,
    notificationDestination: String? = null,
    onNotificationDestinationHandled: () -> Unit = {}
) {
    val user by loginViewModel.userState.collectAsState()
    val isLoading by loginViewModel.isLoading.collectAsState()
    val spreadsheetSetupState by spreadsheetSetupViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val sheetsAccessRefreshTick = remember { mutableIntStateOf(0) }
    val connectedGoogleAccountEmail =
        googleSheetsAuthorizationManager.getSignedInEmail() ?: user?.email
    val hasLoadedSpreadsheetConfiguration = spreadsheetSetupState.hasLoadedConfiguration
    val hasConfiguredSpreadsheet =
        !spreadsheetSetupState.configuredSpreadsheetId.isNullOrBlank() &&
            spreadsheetSetupState.configuredValidationVersion >= SpreadsheetConfig.CURRENT_VALIDATION_VERSION
    val shouldShowSpreadsheetSetup = user != null && hasLoadedSpreadsheetConfiguration && !hasConfiguredSpreadsheet

    val hasGoogleSheetsAccess by produceState<Boolean?>(
        initialValue = if (user == null || !shouldShowSpreadsheetSetup) false else null,
        key1 = user?.uid,
        key2 = shouldShowSpreadsheetSetup,
        key3 = sheetsAccessRefreshTick.intValue
    ) {
        value = if (user == null || !shouldShowSpreadsheetSetup) {
            false
        } else {
            runCatching { googleSheetsAuthorizationManager.hasSheetsAccess() }.getOrDefault(false)
        }
    }
    val isCheckingGoogleSheetsAccess = user != null && hasGoogleSheetsAccess == null
    val requiresGoogleSheetsAccess = user != null && hasGoogleSheetsAccess == false

    val sheetsAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        Log.d(
            "AppRootAuth",
            "Sheets authorization resultCode=${result.resultCode} hasData=${result.data != null}"
        )
        val granted = googleSheetsAuthorizationManager.handleAuthorizationResult(result.data)
        sheetsAccessRefreshTick.intValue++
        if (!granted) {
            Log.w("AppRootAuth", "Google Sheets access was not granted after resolution flow")
            Toast.makeText(context, "Failed to grant Google Sheets access", Toast.LENGTH_SHORT)
                .show()
        }
    }

    when {
        isLoading || (user != null && !hasLoadedSpreadsheetConfiguration) || isCheckingGoogleSheetsAccess -> {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        user != null && hasConfiguredSpreadsheet -> {
            LaundryHubStarter(
                loginViewModel = loginViewModel,
                notificationDestination = notificationDestination,
                onNotificationDestinationHandled = onNotificationDestinationHandled
            )
        }

        shouldShowSpreadsheetSetup -> {
            SpreadsheetSetupScreen(
                state = spreadsheetSetupState,
                connectedAccountEmail = connectedGoogleAccountEmail,
                requiresGoogleSheetsAccess = requiresGoogleSheetsAccess,
                onInputChanged = spreadsheetSetupViewModel::onInputChanged,
                onValidate = spreadsheetSetupViewModel::validateAndContinue,
                onOpenInGoogleSheets = {
                    val spreadsheetUrl = when {
                        spreadsheetSetupState.input.isNotBlank() -> {
                            SpreadsheetIdParser.normalize(spreadsheetSetupState.input)?.let { spreadsheetId ->
                                "https://docs.google.com/spreadsheets/d/$spreadsheetId/edit"
                            } ?: spreadsheetSetupState.input
                        }

                        !spreadsheetSetupState.configuredSpreadsheetUrl.isNullOrBlank() ->
                            spreadsheetSetupState.configuredSpreadsheetUrl

                        !spreadsheetSetupState.configuredSpreadsheetId.isNullOrBlank() ->
                            "https://docs.google.com/spreadsheets/d/${spreadsheetSetupState.configuredSpreadsheetId}/edit"

                        else -> null
                    }

                    if (spreadsheetUrl.isNullOrBlank()) {
                        Toast.makeText(
                            context,
                            "Spreadsheet URL is not available yet",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@SpreadsheetSetupScreen
                    }

                    val uri = Uri.parse(spreadsheetUrl)
                    val sheetsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.docs.editors.sheets")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    val openedSheetsApp = runCatching {
                        context.startActivity(sheetsIntent)
                    }.onFailure {
                        Log.w(
                            "AppRootAuth",
                            "Failed to open Google Sheets app directly for $spreadsheetUrl: ${it.message}",
                            it
                        )
                    }.isSuccess

                    if (!openedSheetsApp) {
                        runCatching {
                            context.startActivity(genericIntent)
                        }.onFailure {
                            Log.e(
                                "AppRootAuth",
                                "Failed to open spreadsheet URL $spreadsheetUrl: ${it.message}",
                                it
                            )
                            Toast.makeText(
                                context,
                                "Unable to open Google Sheets",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onSignOut = {
                    scope.launch {
                        val signedOut = loginViewModel.signOut()
                        if (!signedOut) {
                            Toast.makeText(
                                context,
                                "Failed to sign out",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onGrantGoogleSheetsAccess = {
                    if (activity == null) {
                        Toast.makeText(context, "Unable to open Google authorization", Toast.LENGTH_SHORT)
                            .show()
                        return@SpreadsheetSetupScreen
                    }

                    scope.launch {
                        runCatching {
                            googleSheetsAuthorizationManager.getAuthorizationIntentSender()
                        }.onSuccess { intentSender ->
                            Log.d(
                                "AppRootAuth",
                                "Requested Google Sheets authorization intentSenderAvailable=${intentSender != null}"
                            )
                            if (intentSender != null) {
                                sheetsAuthorizationLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            } else {
                                sheetsAccessRefreshTick.intValue++
                            }
                        }.onFailure {
                            Log.e(
                                "AppRootAuth",
                                "Failed to start Google Sheets authorization: ${it.message}",
                                it
                            )
                            Toast.makeText(
                                context,
                                it.message ?: "Failed to grant Google Sheets access",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }

        else -> {
            // Show onboarding screen
            OnboardingScreen(
                pages = getListOnboardingPage,
                onLoginClick = {
                    if (activity == null) {
                        Toast.makeText(context, "Unable to open Google sign in", Toast.LENGTH_SHORT)
                            .show()
                        return@OnboardingScreen
                    }

                    scope.launch {
                        runCatching {
                            googleCredentialAuthManager.signIn(activity)
                        }.onSuccess { result ->
                            Log.d(
                                "AppRootAuth",
                                "Google sign-in succeeded for email=${result.email}"
                            )
                            loginViewModel.signInGoogle(result.idToken)
                        }.onFailure {
                            Log.e(
                                "AppRootAuth",
                                "Google sign-in failed: ${it.message}",
                                it
                            )
                            Toast.makeText(
                                context,
                                it.message ?: "Failed to sign in",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
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
                if (currentRoute in bottomBarRoutes) {
                    BottomBar(navController, onOrderClick = {
                        showEditOrderSheet.value = false // Pastikan sheet edit tidak aktif
                        orderViewModel.resetForm() // Set mode new order
                        showNewOrderSheet.value = true
                        triggerOpenSheet.value = true
                    })
                }
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
                            scope.launch {
                                homeViewModel.fetchOrder()
                                homeViewModel.fetchTodayIncome()
                                homeViewModel.fetchSummary()
                                homeViewModel.fetchGross()
                            }
                        }
                    )
                }
                composable(BottomNavItem.Outcome.screenRoute) {
                    OutcomeScreenView(
                        bannerState = outcomeBannerState,
                        onOutcomeChanged = {
                            scope.launch {
                                homeViewModel.fetchSummary()
                                homeViewModel.fetchGross()
                            }
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
                    val state by homeViewModel.uiState.collectAsState()
                    GrossDetailScreenView(
                        grossState = state.gross,
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
                    snackBarHostState.showSnackbar(errorMessage)
                    return@launch
                }

                orderViewModel.submitOrder(
                    orderViewModel.uiState.value.toOrderData(orderId),
                    onComplete = {
                        val submittedState = orderViewModel.uiState.value
                        homeViewModel.fetchOrder()
                        homeViewModel.fetchTodayIncome()
                        homeViewModel.fetchSummary()
                        homeViewModel.fetchGross()
                        delay(500)
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
                        snackBarHostState.showSnackbar(successMessage)
                    },
                    onError = { errorMessage ->
                        snackBarHostState.showSnackbar(errorMessage.ifBlank { submitFailedMessage })
                    }
                )
            }
        },
        onUpdate = {
            scope.launch {
                orderViewModel.updateOrder(
                    uiState.toOrderData(uiState.orderID),
                    onComplete = {
                        homeViewModel.fetchOrder()
                        homeViewModel.fetchTodayIncome()
                        homeViewModel.fetchSummary()
                        homeViewModel.fetchGross()
                        delay(500)
                        dismissSheet()
                        snackBarHostState.showSnackbar(
                            context.getString(R.string.order_update_success, uiState.orderID)
                        )
                    },
                    onError = { errorMessage ->
                        snackBarHostState.showSnackbar(errorMessage.ifBlank { updateFailedMessage })
                    }
                )
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
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            Log.d("BottomNavDebug", "Back stack route=${entry.destination.route}")
        }
    }

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
                        Log.d(
                            "BottomNavDebug",
                            "Bottom nav click route=${item.screenRoute} action=open_order_sheet currentRoute=$currentRoute"
                        )
                        onOrderClick()
                    } else {
                        Log.d(
                            "BottomNavDebug",
                            "Bottom nav click route=${item.screenRoute} currentRoute=$currentRoute restoreState=true launchSingleTop=true"
                        )
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
