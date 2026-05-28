package com.deeplink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeplink.app.data.model.VideoDetail
import com.deeplink.app.data.repository.VideoRepository
import com.deeplink.app.data.repository.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val video: VideoDetail? = null
)

class VideoDetailViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailUiState())
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(videoId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, video = null) }
            videoRepository.getVideoDetail(videoId)
                .onSuccess { detail ->
                    _uiState.update { it.copy(isLoading = false, video = detail) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.userMessage())
                    }
                }
        }
    }
}
