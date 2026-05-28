package com.deeplink.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deeplink.app.data.model.TaskStatusResponse
import com.deeplink.app.data.repository.VideoRepository
import com.deeplink.app.data.repository.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FileUploadUiState(
    val isLoading: Boolean = false,
    val isPolling: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val selectedFileName: String? = null,
    val taskStatus: TaskStatusResponse? = null,
    val currentTaskId: String? = null,
    val transcript: String? = null,
    val explanation: String? = null
)

class FileUploadViewModel(
    application: Application,
    private val videoRepository: VideoRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FileUploadUiState())
    val uiState: StateFlow<FileUploadUiState> = _uiState.asStateFlow()

    private var selectedUri: Uri? = null

    fun setSelectedFile(uri: Uri, displayName: String) {
        selectedUri = uri
        _uiState.update {
            it.copy(
                selectedFileName = displayName,
                error = null,
                explanation = null,
                transcript = null,
                taskStatus = null,
                isPolling = false
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun uploadFile(language: String) {
        val uri = selectedUri ?: run {
            _uiState.update { it.copy(error = "Please select a file first") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isPolling = false,
                    error = null,
                    taskStatus = null,
                    transcript = null,
                    explanation = null
                )
            }
            videoRepository.processFile(getApplication(), uri, language)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Upload started",
                            currentTaskId = response.taskId,
                            isPolling = true
                        )
                    }
                    pollTask(response.taskId, response.videoId)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.userMessage())
                    }
                }
        }
    }

    private fun pollTask(taskId: String, initialVideoId: Int?) {
        viewModelScope.launch {
            var lastStatus: TaskStatusResponse? = null
            val outcome = pollTaskStatus(
                videoRepository = videoRepository,
                taskId = taskId,
                pollIntervalMs = 3000L,
                maxDurationMs = 180_000L
            ) { status ->
                lastStatus = status
                _uiState.update { it.copy(taskStatus = status) }
            }

            when (outcome) {
                PollOutcome.Completed -> {
                    lastStatus?.let { fetchAndShowResult(it, initialVideoId) }
                        ?: _uiState.update { it.copy(isPolling = false) }
                }
                PollOutcome.Failed -> _uiState.update { it.copy(isPolling = false) }
                PollOutcome.TimedOut -> {
                    _uiState.update {
                        it.copy(
                            isPolling = false,
                            error = "Processing timeout, check history later"
                        )
                    }
                }
            }
        }
    }

    private fun fetchAndShowResult(status: TaskStatusResponse, fallbackVideoId: Int?) {
        viewModelScope.launch {
            val result = fetchVideoResult(videoRepository, status, fallbackVideoId)
            _uiState.update {
                it.copy(
                    transcript = result.transcript,
                    explanation = result.explanation,
                    isPolling = false
                )
            }
        }
    }
}
