package com.deeplink.app.ui.viewmodel

import com.deeplink.app.data.model.TaskStatusResponse
import com.deeplink.app.data.repository.VideoRepository

internal data class VideoResult(
    val transcript: String? = null,
    val explanation: String? = null
)

internal suspend fun fetchVideoResult(
    videoRepository: VideoRepository,
    status: TaskStatusResponse,
    fallbackVideoId: Int?
): VideoResult {
    val videoId = status.videoId ?: fallbackVideoId
    var transcript: String? = null
    var explanation: String? = null

    if (videoId != null) {
        videoRepository.getVideoDetail(videoId)
            .onSuccess { detail ->
                transcript = detail.transcript
                explanation = detail.explanation
            }
    }

    if (explanation.isNullOrBlank() && transcript.isNullOrBlank()) {
        val result = status.result
        transcript = resultString(result, "transcript")
        explanation = resultString(result, "explanation")
    }

    return VideoResult(transcript = transcript, explanation = explanation)
}

internal fun isTaskDone(status: TaskStatusResponse): Boolean =
    status.status?.lowercase() in listOf("success", "completed", "done", "finished")

internal fun isTaskFailed(status: TaskStatusResponse): Boolean =
    status.status?.lowercase() in listOf("failure", "failed", "error")

private fun resultString(result: Any?, key: String): String? = when (result) {
    is Map<*, *> -> result[key]?.toString()?.takeIf { it.isNotBlank() }
    else -> null
}
