package com.example.mungnyang.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.mungnyang.model.Routes
import com.example.mungnyang.uicomponents.aiCamera.AiCameraDogEye
import com.example.mungnyang.uicomponents.aiCamera.AiCameraDogSkin
import com.example.mungnyang.uicomponents.aiCamera.AiCheckDogEye
import com.example.mungnyang.uicomponents.aiCamera.AiCheckDogSkin
import com.example.mungnyang.uicomponents.main.AiHealth
import com.example.mungnyang.uicomponents.main.PetCalendar
import com.example.mungnyang.uicomponents.main.PetLog
import com.example.mungnyang.uicomponents.main.PetWalk
import com.example.mungnyang.viewmodel.WalkRecordViewModel

fun NavGraphBuilder.mainNavGraph(
    navController: NavController,
    walRecordViewModel: WalkRecordViewModel
) {
    navigation(
        startDestination = Routes.AiHealth.route,
        route = Routes.Main.route
    ) {
        composable(Routes.AiHealth.route) {
            AiHealth(
                onNavigateAiCameraDogEye = { navController.navigate(Routes.AiCameraDogEye.route) },
                onNavigateAiCameraDogSkin = { navController.navigate(Routes.AiCameraDogSkin.route) }
            )
        }
        composable(Routes.AiCameraDogEye.route) {
            AiCameraDogEye(
                onCaptureSuccess = { imageUri ->
                    val encodedUri = android.net.Uri.encode(imageUri)
                    navController.navigate(Routes.AiCheckDogEye().route + "/$encodedUri")
                }
            )
        }
        composable(
            route = Routes.AiCheckDogEye().route + "/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri")
            val imageUri = encodedUri?.let { android.net.Uri.decode(it) }
            AiCheckDogEye(
                capturedImage = imageUri,
                onNavigateAiHealth = { navController.navigate(Routes.AiHealth.route) }
            )
        }

        composable(Routes.AiCameraDogSkin.route) {
            AiCameraDogSkin(
                onCaptureSuccess = { imageUri ->
                    val encodedUri = android.net.Uri.encode(imageUri)
                    navController.navigate(Routes.AiCheckDogSkin().route + "/$encodedUri")
                }
            )
        }
        composable(
            route = Routes.AiCheckDogSkin().route + "/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri")
            val imageUri = encodedUri?.let { android.net.Uri.decode(it) }
            AiCheckDogSkin(
                capturedImage = imageUri,
                onNavigateAiHealth = { navController.navigate(Routes.AiHealth.route) }
            )
        }

        composable(Routes.PetCalendar.route) {
            PetCalendar()
        }
        composable(Routes.PetWalk.route) {
            PetWalk(walRecordViewModel)
        }

        composable(Routes.PetLog.route) {
            PetLog(viewModel = walRecordViewModel)
        }
    }
}