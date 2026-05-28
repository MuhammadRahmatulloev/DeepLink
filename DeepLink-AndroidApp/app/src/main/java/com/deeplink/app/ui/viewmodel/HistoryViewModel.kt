package com.deeplink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeplink.app.data.model.VideoHistoryItem
import com.deeplink.app.data.repository.VideoRepository
import com.deeplink.app.data.repository.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val videos: List<VideoHistoryItem> = emptyList()
)

class HistoryViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false

    fun loadHistory(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return
        val showFullScreenLoader = !hasLoadedOnce && !forceRefresh
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = showFullScreenLoader,
                    isRefreshing = forceRefresh && hasLoadedOnce,
                    error = null
                )
            }
            videoRepository.getHistory()
                .onSuccess { videos ->
                    hasLoadedOnce = true
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            videos = videos
                        )
                    }
                }
                .onFailure { e ->
                    hasLoadedOnce = true
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.userMessage()
                        )
                    }
                }
        }
    }

    fun refreshHistory() = loadHistory(forceRefresh = true)

    fun deleteVideo(id: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            videoRepository.deleteVideo(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isDeleting = false,
                            videos = state.videos.filter { it.id != id }
                        )
                    }
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isDeleting = false, error = e.userMessage())
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
