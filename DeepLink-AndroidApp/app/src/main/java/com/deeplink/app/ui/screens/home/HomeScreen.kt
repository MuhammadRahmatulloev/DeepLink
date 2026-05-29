package com.deeplink.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deeplink.app.data.model.UserProfile
import com.deeplink.app.ui.components.AnimatedPrimaryButton
import com.deeplink.app.ui.components.AppCard
import com.deeplink.app.ui.components.EmptyStateIllustration
import com.deeplink.app.ui.components.GradientTitle
import com.deeplink.app.ui.components.ScreenEnterAnimation
import com.deeplink.app.ui.components.SkeletonBlock
import com.deeplink.app.ui.components.LanguageDropdown
import com.deeplink.app.ui.components.normalizeLanguageCode
import com.deeplink.app.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: UserProfile?,
    homeViewModel: HomeViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToFileUpload: () -> Unit,
    onNavigateToImageOcr: () -> Unit
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var url by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(normalizeLanguageCode(profile?.language)) }

    LaunchedEffect(profile?.language) {
        profile?.language?.let { language = normalizeLanguageCode(it) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            homeViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            homeViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DeepLink") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ScreenEnterAnimation {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                GradientTitle("DeepLink")
                profile?.let {
                    Text(
                        text = "Welcome back, ${it.username ?: it.email ?: "User"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Process YouTube Video", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("YouTube URL") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LanguageDropdown(
                        selectedCode = language,
                        onLanguageSelected = { language = it },
                        enabled = !uiState.isLoading && !uiState.isPolling
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedPrimaryButton(
                        text = "Process Video",
                        onClick = { homeViewModel.processVideo(url.trim(), language) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && !uiState.isPolling && url.isNotBlank(),
                        isLoading = uiState.isLoading
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateToFileUpload,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isPolling
                ) { Text("Upload Document / File") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onNavigateToImageOcr,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isPolling
                ) { Text("Image OCR") }

                if (uiState.isPolling && uiState.explanation == null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Processing...", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(18.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(18.dp))
                    }
                }

                if (uiState.explanation == null && uiState.transcript == null && !uiState.isPolling) {
                    Spacer(modifier = Modifier.height(24.dp))
                    EmptyStateIllustration(
                        icon = Icons.Default.History,
                        title = "No processed result yet",
                        subtitle = "Paste a link to generate transcript and explanation."
                    )
                }

                uiState.transcript?.let {
                    Spacer(modifier = Modifier.height(20.dp))
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Transcript", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                uiState.explanation?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Explanation", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
