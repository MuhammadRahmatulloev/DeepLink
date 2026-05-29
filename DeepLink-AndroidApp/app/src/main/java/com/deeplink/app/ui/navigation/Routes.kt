package com.deeplink.app.ui.navigation

import java.net.URLEncoder

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val VERIFY_EMAIL = "verify_email/{email}"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
    const val HISTORY = "history"
    const val VIDEO_DETAIL = "video_detail/{id}"
    const val FILE_UPLOAD = "file_upload"
    const val IMAGE_OCR = "image_ocr"

    fun videoDetail(id: Int): String = "video_detail/$id"

    fun verifyEmail(email: String): String {
        val encoded = URLEncoder.encode(email, Charsets.UTF_8.name())
        return "verify_email/$encoded"
    }
}
