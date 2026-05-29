package com.deeplink.app.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.deeplink.app.ui.components.AnimatedPrimaryButton
import com.deeplink.app.ui.components.AppCard
import com.deeplink.app.ui.components.GradientTitle
import com.deeplink.app.ui.components.ScreenEnterAnimation
import com.deeplink.app.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        ScreenEnterAnimation {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(42.dp))
                GradientTitle("DeepLink")
                Spacer(modifier = Modifier.height(6.dp))
                Text("AI-powered video and document analyzer")
                Spacer(modifier = Modifier.height(20.dp))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    AnimatedPrimaryButton(
                        text = "Sign In",
                        onClick = { viewModel.login(email.trim(), password, onLoginSuccess) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                        isLoading = uiState.isLoading
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Text("Continue with Google")
                    }
                    TextButton(onClick = onNavigateToForgotPassword) {
                        Text("Forgot password?")
                    }
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Don't have an account? Register")
                    }
                }
            }
        }
    }

}
