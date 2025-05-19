package com.example.mungnyang.uicomponents

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mungnyang.R
import com.example.mungnyang.model.Routes
import com.example.mungnyang.navigation.BottomNavigationBar
import com.example.mungnyang.navigation.NavGraph

@Composable
fun rememberViewModelStoreOwner(): ViewModelStoreOwner {
    val context = LocalContext.current
    return remember(context) {
        when (context) {
            is ViewModelStoreOwner -> context
            else -> error("No ViewModelStoreOwner was provided via LocalContext")
        }
    }
}

val LocalNavGraphViewModelStoreOwner =
    staticCompositionLocalOf<ViewModelStoreOwner> { error("Undefined") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navStoreOwner = rememberViewModelStoreOwner()
    val navController = rememberNavController()

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination by remember(navBackStackEntry) {
        derivedStateOf {
            val route = navBackStackEntry.value?.destination?.route
            Log.d("MainScreen", "Current route: $route")
            route?.let {
                Routes.getRoutes(it)
            } ?: run {
                Routes.Main
            }
        }
    }

    CompositionLocalProvider(LocalNavGraphViewModelStoreOwner provides navStoreOwner) {
        Scaffold(
            topBar = {
                if (currentDestination.isRoot) {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.logo_small_dark),
                                        contentDescription = "Logo",
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "멍냥로그",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                when (currentDestination) {
                                    is Routes.AiCameraDogEye -> {
                                        Text(
                                            text = "닫기",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .clickable { navController.popBackStack() }
                                        )
                                    }
                                    is Routes.AiCameraDogSkin -> {
                                        Text(
                                            text = "닫기",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .clickable { navController.popBackStack() }
                                        )
                                    }
                                    is Routes.AiCheckDogEye -> {
                                        Text(
                                            text = "재촬영",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .clickable { navController.popBackStack() }
                                        )
                                    }
                                    is Routes.AiCheckDogSkin -> {
                                        Text(
                                            text = "재촬영",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .clickable { navController.popBackStack() }
                                        )
                                    }
                                    else -> {
                                        Row(modifier = Modifier.padding(16.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "",
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "",
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarColors(
                            containerColor = Color.White,
                            scrolledContainerColor = Color.White,
                            navigationIconContentColor = Color.Black,
                            titleContentColor = Color.Black,
                            actionIconContentColor = Color.Black
                        )
                    )
                }
            },
            bottomBar = {
                if (currentDestination.isRoot && !currentDestination.isCamera) {
                    BottomNavigationBar(navController)
                }
            }
        ) { contentPadding ->
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}