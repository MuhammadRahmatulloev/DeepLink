package com.deeplink.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.deeplink.app.data.model.UserProfile
import com.deeplink.app.ui.components.AppCard
import com.deeplink.app.ui.components.languageLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = profile?.username?.take(2)?.uppercase() ?: "DL",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ProfileField(label = "Email", value = profile?.email ?: "—")
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileField(label = "Username", value = profile?.username ?: "—")
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileField(
                        label = "Language",
                        value = languageLabel(profile?.language)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileField(
                        label = "Joined",
                        value = profile?.dateJoined ?: "—"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToEditProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToChangePassword,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onToggleTheme,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = null
                )
                Text(
                    text = if (isDarkTheme) " Switch to Light Theme" else " Switch to Dark Theme"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = value, style = MaterialTheme.typography.bodyLarge)
}
