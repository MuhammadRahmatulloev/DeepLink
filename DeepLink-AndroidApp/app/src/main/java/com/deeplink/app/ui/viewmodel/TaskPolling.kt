package com.deeplink.app.ui.viewmodel

import com.deeplink.app.data.model.TaskStatusResponse
import com.deeplink.app.data.repository.VideoRepository
import kotlinx.coroutines.delay

enum class PollOutcome {
    Completed,
    Failed,
    TimedOut
}

internal suspend fun pollTaskStatus(
    videoRepository: VideoRepository,
    taskId: String,
    pollIntervalMs: Long = 2000,
    maxDurationMs: Long = 180_000L,
    onUpdate: (TaskStatusResponse) -> Unit
): PollOutcome {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < maxDurationMs) {
        delay(pollIntervalMs)
        val status = videoRepository.getTaskStatus(taskId).getOrNull() ?: continue
        onUpdate(status)
        when {
            isTaskDone(status) -> return PollOutcome.Completed
            isTaskFailed(status) -> return PollOutcome.Failed
        }
    }
    return PollOutcome.TimedOut
}
