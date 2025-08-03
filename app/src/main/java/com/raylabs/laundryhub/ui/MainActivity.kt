package com.raylabs.laundryhub.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.ui.onboarding.LoginViewModel
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        enableEdgeToEdge()
        setContent {
            LaundryHubTheme { AppRoot() }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun AppPreview() {
        LaundryHubStarter(loginViewModel = hiltViewModel<LoginViewModel>())
    }
}