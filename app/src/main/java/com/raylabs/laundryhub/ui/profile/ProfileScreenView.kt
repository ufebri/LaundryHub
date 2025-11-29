package com.raylabs.laundryhub.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.profile.dummyProfileUiState
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.onboarding.LoginViewModel
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState

@Composable
fun ProfileScreenView(
    viewModel: ProfileViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel,
    onInventoryClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { DefaultTopAppBar("Profile") }
    ) { padding ->
        ProfileScreenContent(
            state = state,
            modifier = Modifier.padding(padding),
            onLoggedOut = {
                viewModel.logOut(onSuccess = {
                    // Handle successful logout, e.g., navigate to login screen
                    loginViewModel.clearUser()
                })
            },
            onInventoryClick = onInventoryClick
        )
    }
}

@Composable
fun ProfileScreenContent(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onLoggedOut: () -> Unit = {},
    onInventoryClick: () -> Unit = {}
) {
    LaunchedEffect(state.logout.data) {
        if (state.logout.data == true) {
            onLoggedOut()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top - User Info Card
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                backgroundColor = Color(0xFF443C56),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = state.user.data?.photoUrl,
                        error = painterResource(R.drawable.ic_branding),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = state.user.data?.displayName ?: "Guest",
                        color = Color.White,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.padding(16.dp))

            Column {
                Text(
                    text = "Store",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    backgroundColor = Color(0xFF4B3F63),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onInventoryClick)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = CircleShape,
                            backgroundColor = Color(0xFFCEC1FF),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = Color(0xFF4B3F63)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Inventory",
                                color = Color.White,
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Setup Order Package",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }
            }
        }

        // Bottom - Version & Sign Out
        Column {
            Card(
                backgroundColor = Color(0xFF443C56),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Version",
                        color = Color.White,
                        style = MaterialTheme.typography.body2
                    )
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        color = Color.White,
                        style = MaterialTheme.typography.body2
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLoggedOut,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B4FAA)),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            ) {
                Text(text = "Sign Out", color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    Scaffold(
        topBar = { DefaultTopAppBar("Profile") }
    ) { padding ->
        ProfileScreenContent(
            state = dummyProfileUiState,
            modifier = Modifier.padding(padding)
        )
    }
}
