package com.deeplink.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.deeplink.app.ui.components.AnimatedPrimaryButton
import com.deeplink.app.ui.components.AppCard
import com.deeplink.app.ui.components.ScreenEnterAnimation
import com.deeplink.app.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onPasswordResetSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val passwordsMatch = newPassword == confirmNewPassword

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    if (step == 1) {
                        Text("Step 1: Send reset code")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        AnimatedPrimaryButton(
                            text = "Send Code",
                            onClick = {
                                viewModel.forgotPassword(email.trim()) {
                                    step = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = uiState.isLoading,
                            enabled = !uiState.isLoading && email.isNotBlank()
                        )
                    } else {
                        Text("Step 2: Reset password")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = {},
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            readOnly = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = { input ->
                                code = input.filter { it.isDigit() }.take(6)
                            },
                            label = { Text("Reset Code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text("Confirm New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        if (!passwordsMatch && confirmNewPassword.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Passwords do not match")
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        AnimatedPrimaryButton(
                            text = "Reset Password",
                            onClick = {
                                viewModel.resetPassword(
                                    email = email.trim(),
                                    code = code.trim(),
                                    newPassword = newPassword
                                ) {
                                    onPasswordResetSuccess()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = uiState.isLoading,
                            enabled = !uiState.isLoading &&
                                code.length == 6 &&
                                newPassword.isNotBlank() &&
                                confirmNewPassword.isNotBlank() &&
                                passwordsMatch
                        )
                    }
                }
            }
        }
    }
}
