package com.raylabs.laundryhub.ui.profile

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import com.raylabs.laundryhub.ui.profile.state.UserItem

@Composable
fun ProfileScreenView(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    
    Scaffold(
        topBar = { DefaultTopAppBar("Profile") }
    ) { padding ->
        ProfileScreenContent(state, modifier = Modifier.padding(padding))
    }
}

@Composable
fun ProfileScreenContent(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onLoggedOut: () -> Unit = {}
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
        Column {
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
        ProfileScreenContent(dummyProfileUiState, modifier = Modifier.padding(padding))
    }
}