package com.deeplink.app.ui.screens.upload

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
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.deeplink.app.data.util.MultipartHelper
import com.deeplink.app.data.util.SupportedUploadMime
import com.deeplink.app.ui.components.ExplanationResultCard
import com.deeplink.app.ui.components.LanguageDropdown
import com.deeplink.app.ui.components.ProcessingCard
import com.deeplink.app.ui.components.normalizeLanguageCode
import com.deeplink.app.ui.viewmodel.FileUploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileUploadScreen(
    viewModel: FileUploadViewModel,
    defaultLanguage: String = "en",
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var language by remember { mutableStateOf(normalizeLanguageCode(defaultLanguage)) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (!SupportedUploadMime.isSupported(context, it)) {
                scope.launch {
                    snackbarHostState.showSnackbar(SupportedUploadMime.unsupportedMessage)
                }
                return@let
            }
            val name = MultipartHelper.getDisplayName(context, it)
            viewModel.setSelectedFile(it, name)
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
                title = { Text("Upload File") },
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
            Text(
                text = "Upload a document or file",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PDF, DOCX, XLSX, TXT, or images",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { filePicker.launch(SupportedUploadMime.PICKER_INPUT) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && !uiState.isPolling
            ) {
                Row(horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Text(
                        text = "Choose File",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            uiState.selectedFileName?.let { fileName ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Selected file",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LanguageDropdown(
                selectedCode = language,
                onLanguageSelected = { language = it },
                enabled = !uiState.isLoading && !uiState.isPolling
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.uploadFile(language) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && !uiState.isPolling && uiState.selectedFileName != null
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Upload & Process")
                }
            }

            if (uiState.isPolling && uiState.explanation == null) {
                Spacer(modifier = Modifier.height(24.dp))
                ProcessingCard(
                    statusText = uiState.taskStatus?.status ?: "pending",
                    errorText = uiState.taskStatus?.error
                )
            }

            uiState.explanation?.let { explanation ->
                Spacer(modifier = Modifier.height(24.dp))
                ExplanationResultCard(explanation = explanation)
            }

            if (uiState.explanation == null && uiState.transcript != null) {
                Spacer(modifier = Modifier.height(24.dp))
                ExplanationResultCard(explanation = uiState.transcript!!)
            }
        }
    }
}
