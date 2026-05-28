package com.deeplink.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deeplink.app.data.repository.VideoRepository
import com.deeplink.app.data.repository.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OcrUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val selectedImageName: String? = null,
    val extractedText: String? = null
)

class OcrViewModel(
    application: Application,
    private val videoRepository: VideoRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private var selectedUri: Uri? = null

    fun setSelectedImage(uri: Uri, displayName: String) {
        selectedUri = uri
        _uiState.update {
            it.copy(
                selectedImageName = displayName,
                extractedText = null,
                error = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun extractText(language: String) {
        val uri = selectedUri ?: run {
            _uiState.update { it.copy(error = "Please select an image first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, extractedText = null) }
            videoRepository.extractImage(getApplication(), uri, language)
                .onSuccess { response ->
                    val text = response.displayText()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            extractedText = text,
                            successMessage = if (text != null) "OCR complete" else "OCR complete (no text found)"
                        )
                    }
                    response.taskId?.let { taskId ->
                        pollTaskForOcr(taskId)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.userMessage())
                    }
                }
        }
    }

    private fun pollTaskForOcr(taskId: String) {
        viewModelScope.launch {
            pollTaskStatus(videoRepository, taskId) { status ->
                val resultText = when (val result = status.result) {
                    is String -> result
                    is Map<*, *> -> result["text"]?.toString()
                        ?: result["extracted_text"]?.toString()
                    else -> null
                }
                if (!resultText.isNullOrBlank()) {
                    _uiState.update { it.copy(extractedText = resultText) }
                }
            }
        }
    }
}
