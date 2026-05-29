package com.deeplink.app.data.repository

import android.content.Context
import android.net.Uri
import com.deeplink.app.data.api.ApiService
import com.deeplink.app.data.model.ExtractImageResponse
import com.deeplink.app.data.model.ProcessVideoRequest
import com.deeplink.app.data.model.ProcessVideoResponse
import com.deeplink.app.data.model.TaskStatusResponse
import com.deeplink.app.data.model.VideoDetail
import com.deeplink.app.data.model.VideoHistoryItem
import com.deeplink.app.data.util.MultipartHelper
import com.google.gson.Gson

class VideoRepository(private val api: ApiService) {

    suspend fun processVideo(url: String, language: String): Result<ProcessVideoResponse> =
        runCatching {
            val response = api.processVideo(ProcessVideoRequest(url, language))
            if (response.isSuccessful) {
                response.body() ?: throw ApiException("Empty response")
            } else {
                throw ApiException(parseError(response.errorBody()?.string()))
            }
        }

    suspend fun getTaskStatus(taskId: String): Result<TaskStatusResponse> = runCatching {
        val response = api.getTaskStatus(taskId)
        if (response.isSuccessful) {
            response.body() ?: throw ApiException("Empty response")
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    suspend fun getHistory(): Result<List<VideoHistoryItem>> = runCatching {
        val response = api.getVideoHistory()
        if (response.isSuccessful) {
            response.body()?.results ?: emptyList()
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    suspend fun getVideoDetail(id: Int): Result<VideoDetail> = runCatching {
        val response = api.getVideoDetail(id)
        if (response.isSuccessful) {
            response.body() ?: throw ApiException("Empty response")
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    suspend fun deleteVideo(id: Int): Result<Unit> = runCatching {
        val response = api.deleteVideo(id)
        if (response.isSuccessful) {
            Unit
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    suspend fun processFile(
        context: Context,
        uri: Uri,
        language: String
    ): Result<ProcessVideoResponse> = runCatching {
        val filePart = MultipartHelper.createFilePart(context, uri, "file")
        val languagePart = MultipartHelper.createLanguagePart(language)
        val response = api.processFile(filePart, languagePart)
        if (response.isSuccessful) {
            response.body() ?: throw ApiException("Empty response")
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    suspend fun extractImage(
        context: Context,
        uri: Uri,
        language: String
    ): Result<ExtractImageResponse> = runCatching {
        val imagePart = MultipartHelper.createFilePart(context, uri, "image")
        val languagePart = MultipartHelper.createLanguagePart(language)
        val response = api.extractImage(imagePart, languagePart)
        if (response.isSuccessful) {
            response.body() ?: throw ApiException("Empty response")
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    private fun parseError(body: String?): String {
        if (body.isNullOrBlank()) return "Request failed"
        return try {
            val map = Gson().fromJson(body, Map::class.java)
            map["detail"]?.toString() ?: body
        } catch (_: Exception) {
            body
        }
    }
}
