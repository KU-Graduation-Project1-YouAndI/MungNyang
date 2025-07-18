package com.example.mungnyang.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mungnyang.model.Routes
import com.example.mungnyang.uicomponents.login.LoginScreen
import com.example.mungnyang.uicomponents.login.WelcomeScreen
import com.example.mungnyang.viewmodel.WalkRecordViewModel

@SuppressLint("ViewModelConstructorInComposable")
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val walkRecordViewModel: WalkRecordViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.Welcome.route
    ) {
        composable(Routes.Welcome.route) {
            WelcomeScreen(onNavigateLogin = { navController.navigate(Routes.Login.route) })
        }
        composable(Routes.Login.route) {
            LoginScreen(onNavigateMain = {
                navController.navigate(Routes.Main.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            })
        }
        mainNavGraph(navController, walkRecordViewModel)
    }
}