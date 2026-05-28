package com.deeplink.app.data.util

import android.content.Context
import android.net.Uri

object SupportedUploadMime {

    private val exactMimeTypes = setOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain"
    )

    const val PICKER_INPUT = "*/*"

    const val unsupportedMessage =
        "Unsupported file type. Choose PDF, DOCX, XLSX, TXT, or an image."

    fun isSupported(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri) ?: return false
        return mimeType in exactMimeTypes || mimeType.startsWith("image/")
    }
}
