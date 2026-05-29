package com.deeplink.app.data.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    @SerializedName("password2") val password2: String,
    val language: String = "en"
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    @SerializedName("new_password") val newPassword: String
)

data class UpdateProfileRequest(
    val username: String,
    val language: String
)

data class ChangePasswordRequest(
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("new_password2") val newPassword2: String
)

data class LogoutRequest(
    val refresh: String
)

data class TokenResponse(
    val access: String,
    val refresh: String
)

data class UserProfile(
    val id: Int? = null,
    val email: String? = null,
    val username: String? = null,
    val language: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean? = null,
    @SerializedName("date_joined") val dateJoined: String? = null
)

data class MessageResponse(
    val detail: String? = null,
    val message: String? = null
)
