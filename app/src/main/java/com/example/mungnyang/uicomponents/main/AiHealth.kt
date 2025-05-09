package com.example.mungnyang.uicomponents.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mungnyang.R

//@Preview(showBackground = true)
@Composable
fun AiHealth(
    onNavigateAiCameraDogEye: ()->Unit,
    onNavigateAiCameraDogSkin: ()->Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            Text(
                text = "AI 카메라로 내 아이의 질환을 직접 체크해보세요",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(60.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ai_dog_eye),
                    contentDescription = "",
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { onNavigateAiCameraDogEye() }
                )
                Spacer(modifier = Modifier.height(36.dp))
                Image(
                    painter = painterResource(id = R.drawable.ai_dog_skin),
                    contentDescription = "",
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { onNavigateAiCameraDogSkin() }
                )
            }
        }
    }
}