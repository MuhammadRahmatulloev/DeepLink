package com.deeplink.app.ui.screens.ocr

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.deeplink.app.data.util.MultipartHelper
import com.deeplink.app.ui.components.AnimatedPrimaryButton
import com.deeplink.app.ui.components.AppCard
import com.deeplink.app.ui.viewmodel.OcrViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageOcrScreen(
    viewModel: OcrViewModel,
    defaultLanguage: String = "en",
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var language by remember { mutableStateOf(defaultLanguage) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = MultipartHelper.getDisplayName(context, it)
            viewModel.setSelectedImage(it, name)
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image OCR") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Extract text from image", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Icon(Icons.Default.Collections, contentDescription = null)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Text(
                        text = uiState.selectedImageName ?: "Choose Image",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("Language") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedPrimaryButton(
                text = "Extract Text",
                onClick = { viewModel.extractText(language.trim()) },
                modifier = Modifier.fillMaxWidth(),
                isLoading = uiState.isLoading,
                enabled = !uiState.isLoading && uiState.selectedImageName != null
            )

            uiState.extractedText?.let { text ->
                Spacer(modifier = Modifier.height(24.dp))
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Extracted Text",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
