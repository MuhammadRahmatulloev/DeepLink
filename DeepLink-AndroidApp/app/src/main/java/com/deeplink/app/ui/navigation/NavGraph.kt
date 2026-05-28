package com.deeplink.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deeplink.app.ui.screens.auth.LoginScreen
import com.deeplink.app.ui.screens.auth.RegisterScreen
import com.deeplink.app.ui.screens.auth.VerifyEmailScreen
import com.deeplink.app.ui.screens.history.HistoryScreen
import com.deeplink.app.ui.screens.history.VideoDetailScreen
import com.deeplink.app.ui.screens.home.HomeScreen
import com.deeplink.app.ui.screens.ocr.ImageOcrScreen
import com.deeplink.app.ui.screens.profile.ProfileScreen
import com.deeplink.app.ui.screens.upload.FileUploadScreen
import com.deeplink.app.ui.viewmodel.AuthViewModel
import com.deeplink.app.ui.viewmodel.FileUploadViewModel
import com.deeplink.app.ui.viewmodel.HistoryViewModel
import com.deeplink.app.ui.viewmodel.HomeViewModel
import com.deeplink.app.ui.viewmodel.OcrViewModel
import com.deeplink.app.ui.viewmodel.VideoDetailViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun DeepLinkNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    historyViewModel: HistoryViewModel,
    fileUploadViewModel: FileUploadViewModel,
    ocrViewModel: OcrViewModel,
    videoDetailViewModel: VideoDetailViewModel,
    startDestination: String
) {
    val authState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVerify = { email ->
                    navController.navigate(Routes.verifyEmail(email)) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.VERIFY_EMAIL,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedEmail = backStackEntry.arguments?.getString("email") ?: ""
            val email = URLDecoder.decode(encodedEmail, StandardCharsets.UTF_8.toString())
            VerifyEmailScreen(
                email = email,
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.VERIFY_EMAIL) { inclusive = true }
                    }
                },
                onVerified = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.VERIFY_EMAIL) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                profile = authState.profile,
                homeViewModel = homeViewModel,
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToFileUpload = { navController.navigate(Routes.FILE_UPLOAD) },
                onNavigateToImageOcr = { navController.navigate(Routes.IMAGE_OCR) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                profile = authState.profile,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.HISTORY) {
            val refreshOnOpen by homeViewModel.historyRefreshPending.collectAsState()
            HistoryScreen(
                viewModel = historyViewModel,
                refreshOnOpen = refreshOnOpen,
                onRefreshConsumed = { homeViewModel.clearHistoryRefreshPending() },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id ->
                    navController.navigate(Routes.videoDetail(id))
                }
            )
        }

        composable(
            route = Routes.VIDEO_DETAIL,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getInt("id") ?: return@composable
            VideoDetailScreen(
                videoId = videoId,
                viewModel = videoDetailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FILE_UPLOAD) {
            FileUploadScreen(
                viewModel = fileUploadViewModel,
                defaultLanguage = authState.profile?.language ?: "en",
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.IMAGE_OCR) {
            ImageOcrScreen(
                viewModel = ocrViewModel,
                defaultLanguage = authState.profile?.language ?: "en",
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
