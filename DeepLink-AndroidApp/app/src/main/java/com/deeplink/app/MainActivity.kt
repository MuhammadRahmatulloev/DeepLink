package com.deeplink.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deeplink.app.ui.navigation.DeepLinkNavGraph
import com.deeplink.app.ui.navigation.Routes
import com.deeplink.app.ui.theme.DeepLinkTheme
import com.deeplink.app.ui.viewmodel.AuthViewModel
import com.deeplink.app.ui.viewmodel.FileUploadViewModel
import com.deeplink.app.ui.viewmodel.HistoryViewModel
import com.deeplink.app.ui.viewmodel.HomeViewModel
import com.deeplink.app.ui.viewmodel.OcrViewModel
import com.deeplink.app.ui.viewmodel.VideoDetailViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DeepLinkApp
        val startDestination = if (app.authRepository.isLoggedIn()) Routes.HOME else Routes.LOGIN

        setContent {
            var darkThemeEnabled by rememberSaveable { mutableStateOf(true) }
            DeepLinkTheme(darkTheme = darkThemeEnabled) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authViewModel: AuthViewModel = viewModel(
                        factory = ViewModelFactory { AuthViewModel(app.authRepository) }
                    )
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = ViewModelFactory { HomeViewModel(app.videoRepository) }
                    )
                    val historyViewModel: HistoryViewModel = viewModel(
                        factory = ViewModelFactory { HistoryViewModel(app.videoRepository) }
                    )
                    val fileUploadViewModel: FileUploadViewModel = viewModel(
                        factory = ViewModelFactory {
                            FileUploadViewModel(application, app.videoRepository)
                        }
                    )
                    val ocrViewModel: OcrViewModel = viewModel(
                        factory = ViewModelFactory {
                            OcrViewModel(application, app.videoRepository)
                        }
                    )
                    val videoDetailViewModel: VideoDetailViewModel = viewModel(
                        factory = ViewModelFactory {
                            VideoDetailViewModel(app.videoRepository)
                        }
                    )

                    DeepLinkNavGraph(
                        authViewModel = authViewModel,
                        homeViewModel = homeViewModel,
                        historyViewModel = historyViewModel,
                        fileUploadViewModel = fileUploadViewModel,
                        ocrViewModel = ocrViewModel,
                        videoDetailViewModel = videoDetailViewModel,
                        isDarkTheme = darkThemeEnabled,
                        onToggleTheme = { darkThemeEnabled = !darkThemeEnabled },
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
