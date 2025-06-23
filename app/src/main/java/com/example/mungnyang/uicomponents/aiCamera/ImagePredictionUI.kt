package com.example.mungnyang.uicomponents.aiCamera

import android.util.Log
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mungnyang.model.fd.FederateLearning
import org.tensorflow.lite.Interpreter
import java.io.IOException

@Composable
fun ImagePredictionUI(
    tflite: Interpreter,
    imgBitmap: Bitmap
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var prediction by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = {
            // 이미지 로드
            bitmap = imgBitmap

            bitmap?.let {
                prediction = runModelInference(tflite, it)
                Log.d("CHECK", "📦 runTraining 호출됨")
                FederateLearning.runTraining(it, tflite, context)
            }
        }) {
            Text("Run Prediction")
        }

        Spacer(modifier = Modifier.height(16.dp))

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Prediction: $prediction", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun loadImageFromAssets(context: Context, fileName: String): Bitmap? {
    return try {
        val inputStream = context.assets.open(fileName)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

private fun runModelInference(tflite: Interpreter, bitmap: Bitmap): String {
    val input = preprocessBitmap(bitmap)
    val output = Array(1) { FloatArray(7) } // 모델의 클래스 개수

    tflite.run(input, output)

    // 가장 높은 가능성을 갖는 클래스 도출
    val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
    val classLabels = listOf(
        "구진 플라크",
        "비듬/각질/상피성잔고리",
        "태선화/과다색소침착",
        "농포/여드름",
        "미란/궤양",
        "결절/종괴",
        "무증상"
    )
    val className = if (maxIndex in classLabels.indices) classLabels[maxIndex] else "알 수 없음"
    return "Class $maxIndex: $className with probability ${output[0][maxIndex]}"
}

private fun preprocessBitmap(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true) // Adjust size based on your model
    val input = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }
    for (x in 0 until 224) {
        for (y in 0 until 224) {
            val pixel = resizedBitmap.getPixel(x, y)
            input[0][x][y][0] = (pixel shr 16 and 0xFF) / 255.0f
            input[0][x][y][1] = (pixel shr 8 and 0xFF) / 255.0f
            input[0][x][y][2] = (pixel and 0xFF) / 255.0f
        }
    }
    return input
}
