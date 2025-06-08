package com.raylabs.laundryhub.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.raylabs.laundryhub.core.domain.usecase.auth.CheckUserLoggedInUseCase
import com.raylabs.laundryhub.ui.login.LoginScreen
import com.raylabs.laundryhub.ui.login.LoginViewModel
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var checkUserLoggedInUseCase: CheckUserLoggedInUseCase

    @Inject
    lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        // Cek apakah user sudah login
        val isAlreadyLogin = checkUserLoggedInUseCase()

        enableEdgeToEdge()
        setContent {
            LaundryHubTheme {
                if (isAlreadyLogin)
                    LaundryHubStarter()
                else
                    LoginScreen(onLoginClicked = { signIn() })
            }
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account = task.result
                val idToken = account?.idToken
                if (!idToken.isNullOrEmpty()) {
                    viewModel.signInGoogle(idToken)
                } else {
                    // handle error
                    Toast.makeText(
                        this,
                        "ID Token: $idToken",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Google sign-in failed: ${task.result}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    @Preview(showBackground = true)
    @Composable
    fun AppPreview() {
        LaundryHubStarter()
    }
}