package com.deeplink.app.data.api

import com.deeplink.app.data.model.ExtractImageResponse
import com.deeplink.app.data.model.ChangePasswordRequest
import com.deeplink.app.data.model.GoogleSignInRequest
import com.deeplink.app.data.model.ForgotPasswordRequest
import com.deeplink.app.data.model.LoginRequest
import com.deeplink.app.data.model.LogoutRequest
import com.deeplink.app.data.model.MessageResponse
import com.deeplink.app.data.model.ProcessVideoRequest
import com.deeplink.app.data.model.ProcessVideoResponse
import com.deeplink.app.data.model.RegisterRequest
import com.deeplink.app.data.model.ResetPasswordRequest
import com.deeplink.app.data.model.TaskStatusResponse
import com.deeplink.app.data.model.TokenResponse
import com.deeplink.app.data.model.UpdateProfileRequest
import com.deeplink.app.data.model.UserProfile
import com.deeplink.app.data.model.VerifyEmailRequest
import com.deeplink.app.data.model.VideoDetail
import com.deeplink.app.data.model.VideoHistoryItem
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("auth/register/")
    suspend fun register(@Body body: RegisterRequest): Response<MessageResponse>

    @POST("auth/verify-email/")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): Response<MessageResponse>

    @POST("auth/login/")
    suspend fun login(@Body body: LoginRequest): Response<TokenResponse>

    @POST("auth/google/")
    suspend fun googleSignIn(@Body body: GoogleSignInRequest): Response<TokenResponse>

    @POST("auth/forgot-password/")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<MessageResponse>

    @POST("auth/reset-password/")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<MessageResponse>

    @POST("auth/logout/")
    suspend fun logout(@Body body: LogoutRequest): Response<Unit>

    @GET("auth/profile/")
    suspend fun getProfile(): Response<UserProfile>

    @PATCH("auth/profile/")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<UserProfile>

    @POST("auth/change-password/")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<MessageResponse>

    @POST("videos/process/")
    suspend fun processVideo(@Body body: ProcessVideoRequest): Response<ProcessVideoResponse>

    @GET("videos/task/{task_id}/")
    suspend fun getTaskStatus(@Path("task_id") taskId: String): Response<TaskStatusResponse>

    @GET("videos/history/")
    suspend fun getVideoHistory(): Response<List<VideoHistoryItem>>

    @GET("videos/{id}/")
    suspend fun getVideoDetail(@Path("id") id: Int): Response<VideoDetail>

    @DELETE("videos/{id}/")
    suspend fun deleteVideo(@Path("id") id: Int): Response<Unit>

    @Multipart
    @POST("videos/process-file/")
    suspend fun processFile(
        @Part file: MultipartBody.Part,
        @Part("language") language: RequestBody
    ): Response<ProcessVideoResponse>

    @Multipart
    @POST("videos/extract-image/")
    suspend fun extractImage(
        @Part image: MultipartBody.Part,
        @Part("language") language: RequestBody? = null
    ): Response<ExtractImageResponse>
}
