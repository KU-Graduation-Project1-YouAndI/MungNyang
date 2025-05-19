package com.example.mungnyang.navigation

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mungnyang.model.NavBarItems
import com.example.mungnyang.model.Routes

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = Color(0xFFFFF2C2)
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        Log.d("루트루트", currentRoute.toString())

        NavBarItems.BarItems.forEach { navItem ->
            NavigationBarItem(
                selected = navItem.routes.any { route -> 
                    currentRoute == route || 
                    (route == "AiCheckDogEye" && currentRoute?.startsWith("AiCheckDogEye") == true) ||
                    (route == "AiCheckDogSkin" && currentRoute?.startsWith("AiCheckDogSkin") == true)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Unspecified,
                    unselectedIconColor = Color.Unspecified,
                    selectedTextColor =
                        if (navItem.routes.contains(currentRoute)) Color(0xFF854C22)
                        else Color(0xFFE3B7A0),
                    unselectedTextColor = Color(0xFFE3B7A0),
                    indicatorColor = Color.Transparent
                ),
                onClick = {
                    navController.navigate(navItem.routes.first()) {
                        popUpTo(Routes.AiHealth.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            id =
                                if (navItem.routes.any { route -> 
                                    currentRoute == route || 
                                    (route == "AiCheckDogEye" && currentRoute?.startsWith("AiCheckDogEye") == true) ||
                                    (route == "AiCheckDogSkin" && currentRoute?.startsWith("AiCheckDogSkin") == true)
                                }) navItem.onSelectIcon
                                else navItem.unSelectIcon
                        ),
                        contentDescription = navItem.title,
                        modifier = Modifier.size(30.dp),
                        tint = Color.Unspecified
                    )
                },
                label = { 
                    Text(
                        text = navItem.title,
                        color =
                            if (navItem.routes.any { route -> 
                                currentRoute == route || 
                                (route == "AiCheckDogEye" && currentRoute?.startsWith("AiCheckDogEye") == true) ||
                                (route == "AiCheckDogSkin" && currentRoute?.startsWith("AiCheckDogSkin") == true)
                            }) Color(0xFF854C22)
                            else Color(0xFFE3B7A0)
                    )
                }
            )
        }
    }
}

@Preview
@Composable
private fun prev() {
    val navController = rememberNavController()
    BottomNavigationBar(navController)
}