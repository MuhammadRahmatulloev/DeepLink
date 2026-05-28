package com.deeplink.app

import android.app.Application
import com.deeplink.app.data.api.RetrofitClient
import com.deeplink.app.data.local.TokenManager
import com.deeplink.app.data.repository.AuthRepository
import com.deeplink.app.data.repository.VideoRepository

class DeepLinkApp : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var videoRepository: VideoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        val api = RetrofitClient.createApiService(tokenManager)
        authRepository = AuthRepository(api, tokenManager)
        videoRepository = VideoRepository(api)
    }
}
