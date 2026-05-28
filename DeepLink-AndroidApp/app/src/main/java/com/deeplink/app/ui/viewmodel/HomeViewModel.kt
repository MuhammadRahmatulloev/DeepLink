package com.deeplink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeplink.app.data.model.TaskStatusResponse
import com.deeplink.app.data.repository.VideoRepository
import com.deeplink.app.data.repository.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val isPolling: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val taskStatus: TaskStatusResponse? = null,
    val currentTaskId: String? = null,
    val transcript: String? = null,
    val explanation: String? = null
)

class HomeViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _historyRefreshPending = MutableStateFlow(false)
    val historyRefreshPending: StateFlow<Boolean> = _historyRefreshPending.asStateFlow()

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun clearHistoryRefreshPending() {
        _historyRefreshPending.value = false
    }

    private fun markHistoryRefreshNeeded() {
        _historyRefreshPending.value = true
    }

    fun processVideo(url: String, language: String) {
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
            videoRepository.processVideo(url, language)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Processing started",
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
                    markHistoryRefreshNeeded()
                    lastStatus?.let { fetchAndShowResult(it, initialVideoId) }
                        ?: _uiState.update { it.copy(isPolling = false) }
                }
                PollOutcome.Failed -> {
                    markHistoryRefreshNeeded()
                    _uiState.update { it.copy(isPolling = false) }
                }
                PollOutcome.TimedOut -> {
                    markHistoryRefreshNeeded()
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
