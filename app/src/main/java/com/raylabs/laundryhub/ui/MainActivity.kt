package com.raylabs.laundryhub.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.raylabs.laundryhub.core.domain.usecase.update.CheckAppUpdateUseCase
import com.raylabs.laundryhub.ui.onboarding.LoginViewModel
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var checkAppUpdate: CheckAppUpdateUseCase
    private var hasCheckedUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        enableEdgeToEdge()
        setContent {
            LaundryHubTheme { AppRoot() }
        }
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
}