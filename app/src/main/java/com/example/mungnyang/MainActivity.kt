package com.example.mungnyang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mungnyang.ui.theme.MungNyangTheme
import com.example.mungnyang.uicomponents.MainScreen

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MungNyangTheme {
                MainScreen()
            }
        }
    }
}