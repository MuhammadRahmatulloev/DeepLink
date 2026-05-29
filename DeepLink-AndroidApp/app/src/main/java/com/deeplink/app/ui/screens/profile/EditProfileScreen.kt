package com.deeplink.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.deeplink.app.ui.components.AnimatedPrimaryButton
import com.deeplink.app.ui.components.AppCard
import com.deeplink.app.ui.components.LanguageDropdown
import com.deeplink.app.ui.components.normalizeLanguageCode
import com.deeplink.app.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var username by remember(uiState.profile?.username) {
        mutableStateOf(uiState.profile?.username.orEmpty())
    }
    var language by remember(uiState.profile?.language) {
        mutableStateOf(normalizeLanguageCode(uiState.profile?.language))
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                LanguageDropdown(
                    selectedCode = language,
                    onLanguageSelected = { language = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
                AnimatedPrimaryButton(
                    text = "Save",
                    onClick = { viewModel.updateProfile(username.trim(), language) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState.isLoading,
                    enabled = !uiState.isLoading && username.isNotBlank()
                )
            }
        }
    }
}
