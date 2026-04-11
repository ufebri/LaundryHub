package com.raylabs.laundryhub.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.raylabs.laundryhub.core.domain.usecase.reminder.EnsureReminderScheduleUseCase
import com.raylabs.laundryhub.core.domain.usecase.update.CheckAppUpdateUseCase
import com.raylabs.laundryhub.core.reminder.ReminderNotificationConfig
import com.raylabs.laundryhub.ui.onboarding.LoginViewModel
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var checkAppUpdate: CheckAppUpdateUseCase

    @Inject
    lateinit var ensureReminderScheduleUseCase: EnsureReminderScheduleUseCase

    private var hasCheckedUpdate = false
    private var pendingNotificationDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        pendingNotificationDestination = extractReminderDestination(intent)

        enableEdgeToEdge()
        setContent {
            LaundryHubTheme {
                AppRoot(
                    notificationDestination = pendingNotificationDestination,
                    onNotificationDestinationHandled = {
                        pendingNotificationDestination = null
                    }
                )
            }
        }

        lifecycleScope.launch {
            ensureReminderScheduleUseCase()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingNotificationDestination = extractReminderDestination(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!hasCheckedUpdate) {
            hasCheckedUpdate = true
            lifecycleScope.launch {
                checkAppUpdate()
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun AppPreview() {
        LaundryHubStarter(loginViewModel = hiltViewModel<LoginViewModel>())
    }

    private fun extractReminderDestination(intent: Intent?): String? {
        return intent?.getStringExtra(ReminderNotificationConfig.EXTRA_DESTINATION)
    }
}
