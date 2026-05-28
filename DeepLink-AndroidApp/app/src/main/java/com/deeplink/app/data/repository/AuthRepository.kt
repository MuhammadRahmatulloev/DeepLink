package com.deeplink.app.data.repository

import com.deeplink.app.data.api.ApiService
import com.deeplink.app.data.local.TokenManager
import com.deeplink.app.data.model.LoginRequest
import com.deeplink.app.data.model.LogoutRequest
import com.deeplink.app.data.model.RegisterRequest
import com.deeplink.app.data.model.UserProfile
import com.deeplink.app.data.model.VerifyEmailRequest
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val api: ApiService,
    private val tokenManager: TokenManager
) {

    suspend fun register(
        email: String,
        username: String,
        password: String,
        password2: String,
        language: String
    ): Result<String> = runCatching {
        val response = api.register(
            RegisterRequest(email, username, password, password2, language)
        )
        if (response.isSuccessful) {
            response.body()?.detail ?: response.body()?.message ?: "Registration successful"
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    suspend fun verifyEmail(email: String, code: String): Result<String> = runCatching {
        val response = api.verifyEmail(VerifyEmailRequest(email, code))
        if (response.isSuccessful) {
            response.body()?.detail ?: response.body()?.message ?: "Email verified"
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> = runCatching {
        val response = api.login(LoginRequest(email, password))
        if (response.isSuccessful) {
            val tokens = response.body() ?: throw ApiException("Empty response")
            tokenManager.saveTokens(tokens.access, tokens.refresh)
            getProfile().getOrThrow()
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    suspend fun logout(): Result<Unit> = runCatching {
        val refresh = tokenManager.refreshToken
        if (!refresh.isNullOrBlank()) {
            try {
                api.logout(LogoutRequest(refresh))
            } catch (_: Exception) {
                // Clear local tokens even if server logout fails
            }
        }
        tokenManager.clearTokens()
    }

    suspend fun getProfile(): Result<UserProfile> = runCatching {
        val response = api.getProfile()
        if (response.isSuccessful) {
            response.body() ?: throw ApiException("Empty profile")
        } else {
            throw ApiException(parseError(response.errorBody()?.string()))
        }
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn

    private fun parseError(body: String?): String {
        if (body.isNullOrBlank()) return "Request failed"
        return try {
            val map = Gson().fromJson(body, Map::class.java)
            when {
                map["detail"] != null -> map["detail"].toString()
                map["email"] is List<*> -> (map["email"] as List<*>).firstOrNull().toString()
                map["password"] is List<*> -> (map["password"] as List<*>).firstOrNull().toString()
                map["non_field_errors"] is List<*> ->
                    (map["non_field_errors"] as List<*>).firstOrNull().toString()
                else -> body
            }
        } catch (_: Exception) {
            body
        }
    }
}

class ApiException(message: String) : Exception(message)

fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Error"
    is HttpException -> "Server error (${code()})"
    is IOException -> "Network error. Check your connection."
    else -> message ?: "Unknown error"
}
