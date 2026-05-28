package com.deeplink.app.data.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

object MultipartHelper {

    fun createFilePart(context: Context, uri: Uri, partName: String): MultipartBody.Part {
        val (file, mimeType) = uriToTempFile(context, uri)
        val body = file.asRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, body)
    }

    fun createLanguagePart(language: String): RequestBody =
        language.toRequestBody("text/plain".toMediaTypeOrNull())

    fun getDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex) ?: uri.lastPathSegment ?: "Selected file"
            }
        }
        return uri.lastPathSegment ?: "Selected file"
    }

    private fun uriToTempFile(context: Context, uri: Uri): Pair<File, String> {
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = getDisplayName(context, uri).replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("Cannot read file")
        return tempFile to mimeType
    }
}
