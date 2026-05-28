package com.deeplink.app.data.model

import com.google.gson.annotations.SerializedName

data class ProcessVideoRequest(
    val url: String,
    val language: String = "en"
)

data class ProcessVideoResponse(
    @SerializedName("task_id") val taskId: String,
    @SerializedName("video_id") val videoId: Int? = null
)

data class TaskStatusResponse(
    @SerializedName("task_id") val taskId: String? = null,
    val status: String? = null,
    val result: Any? = null,
    val error: String? = null,
    @SerializedName("video_id") val videoId: Int? = null
)

data class VideoHistoryItem(
    val id: Int,
    val url: String? = null,
    val title: String? = null,
    val status: String? = null,
    val language: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class VideoDetail(
    val id: Int,
    val url: String? = null,
    val title: String? = null,
    val status: String? = null,
    val language: String? = null,
    val transcript: String? = null,
    val explanation: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class ExtractImageResponse(
    val text: String? = null,
    @SerializedName("extracted_text") val extractedText: String? = null,
    @SerializedName("ocr_text") val ocrText: String? = null,
    val result: String? = null,
    @SerializedName("task_id") val taskId: String? = null,
    @SerializedName("video_id") val videoId: Int? = null,
    val language: String? = null
) {
    fun displayText(): String? =
        text ?: extractedText ?: ocrText ?: result
}
