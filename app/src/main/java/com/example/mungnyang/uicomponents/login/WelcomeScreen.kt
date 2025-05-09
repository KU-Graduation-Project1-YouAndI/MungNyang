package com.example.mungnyang.uicomponents.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.mungnyang.R
import com.example.mungnyang.model.Routes
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onNavigateLogin: ()->Unit,
    modifier: Modifier = Modifier.fillMaxSize().fillMaxWidth()
) {
    Image(
        painter = painterResource(id = R.drawable.splash),
        contentDescription = null,
        modifier = Modifier.fillMaxSize().fillMaxWidth().padding(0.dp)
    )
    LaunchedEffect(Unit) {
        delay(2000)
        onNavigateLogin()
    }
}

@Preview
@Composable
private fun Prev() {
    val navController = rememberNavController()
    WelcomeScreen(onNavigateLogin = { navController.navigate(Routes.Login.route) })
}