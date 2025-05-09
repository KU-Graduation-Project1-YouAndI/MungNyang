package com.example.mungnyang.uicomponents.aiCamera

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private fun loadBitmap(context: android.content.Context, uriString: String): android.graphics.Bitmap? {
    return try {
        val uri = Uri.parse(uriString)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e("AiCheckDogEye", "이미지 로드 실패", e)
        null
    }
}

@Composable
fun AiCheckDogEye(
    modifier: Modifier = Modifier,
    capturedImage: String?
) {
    val context = LocalContext.current
    val bitmap = remember(capturedImage) {
        capturedImage?.let { loadBitmap(context, it) }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFF2C2)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "촬영된 이미지",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(700.dp)
                    .padding(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(700.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("이미지를 불러올 수 없습니다.")
            }
        }
    }
}