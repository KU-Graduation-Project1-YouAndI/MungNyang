package com.example.mungnyang.uicomponents.aiCamera

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mungnyang.model.fd.FederateLearning
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.concurrent.thread

private fun sliceString(input: String, startIndex: Int): String {
    val sub = input.substring(startIndex)
    val firstSpace = sub.indexOf(' ')
    return if (firstSpace == -1) sub else sub.substring(0, firstSpace)
}

private fun sliceProbability(input: String): String {
    val keyword = "with probability"
    val index = input.indexOf(keyword)
    return input.substring(index)
}

private fun loadBitmap(
    context: android.content.Context,
    uriString: String
): android.graphics.Bitmap? {
    return try {
        val uri = Uri.parse(uriString)
        // 원본 이미지 URI에서 AI 이미지 URI로 변환
        val aiUriString = uriString.replace("original_", "ai_")
        val aiUri = Uri.parse(aiUriString)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, aiUri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, aiUri)
        }
    } catch (e: Exception) {
        Log.e("AiCheckDogSkin", "이미지 로드 실패", e)
        null
    }
}

private fun loadModelFile(assetManager: AssetManager, modelFileName: String): MappedByteBuffer? {
    return try {
        val fileDescriptor = assetManager.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val result = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
        inputStream.close()
        fileChannel.close()
        result
    } catch (e: Exception) {
        Log.e("AiCheckDogSkin", "모델 파일 로드 실패", e)
        null
    }
}

//=======================================================================
private fun loadImageFromAssets(context: Context, fileName: String): Bitmap? {
    return try {
        val inputStream = context.assets.open(fileName)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

private fun runModelInference(tflite: Interpreter?, bitmap: Bitmap): String {
    if (tflite == null) {
        return "모델 인터프리터가 초기화되지 않았습니다."
    }

    return try {
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
        "Class $maxIndex: $className with probability ${output[0][maxIndex]}"
    } catch (e: Exception) {
        Log.e("AiCheckDogSkin", "모델 추론 실패", e)
        "예측 중 오류가 발생했습니다: ${e.message}"
    }
}

private fun preprocessBitmap(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
    // 하드웨어 가속 비트맵을 소프트웨어 비트맵으로 변환
    val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, true)
    } else {
        bitmap
    }

    val resizedBitmap =
        Bitmap.createScaledBitmap(softwareBitmap, 224, 224, true) // Adjust size based on your model
    val input = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }

    try {
        for (x in 0 until 224) {
            for (y in 0 until 224) {
                val pixel = resizedBitmap.getPixel(x, y)
                input[0][x][y][0] = (pixel shr 16 and 0xFF) / 255.0f
                input[0][x][y][1] = (pixel shr 8 and 0xFF) / 255.0f
                input[0][x][y][2] = (pixel and 0xFF) / 255.0f
            }
        }

        // 원본 비트맵과 다른 경우 리사이즈된 비트맵 리소스 해제
        if (softwareBitmap != bitmap) {
            softwareBitmap.recycle()
        }
        if (resizedBitmap != softwareBitmap) {
            resizedBitmap.recycle()
        }
    } catch (e: Exception) {
        Log.e("AiCheckDogSkin", "비트맵 전처리 중 오류 발생", e)
        throw e
    }

    return input
}
//=======================================================================

@Composable
fun AiCheckDogSkin(
    modifier: Modifier = Modifier,
    capturedImage: String?,
    onNavigateAiHealth: () -> Unit
) {
    val context = LocalContext.current
    var bitmap = remember(capturedImage) {
        capturedImage?.let { loadBitmap(context, it) }
    }

    // TFLite 인터프리터를 remember를 사용해 한 번만 초기화
    var tflite by remember { mutableStateOf<Interpreter?>(null) }
    var isModelLoaded by remember { mutableStateOf(false) }
    var prediction by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var trainingLaunched by remember { mutableStateOf(false) }


    DisposableEffect(key1 = context) {
        try {
            val modelBuffer =
                loadModelFile(assetManager = context.assets, modelFileName = "model.tflite")
            if (modelBuffer != null) {
                tflite = Interpreter(modelBuffer)
                isModelLoaded = true
            } else {
                errorMessage = "모델 파일을 로드할 수 없습니다."
            }
        } catch (e: Exception) {
            Log.e("AiCheckDogSkin", "TFLite 인터프리터 초기화 실패", e)
            errorMessage = "모델 초기화 중 오류가 발생했습니다: ${e.message}"
        }

        onDispose {
            tflite?.close()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(140.dp))

        Column(modifier = Modifier.padding(16.dp)) {
            if (bitmap != null && isModelLoaded && !trainingLaunched) {
                trainingLaunched = true

               // prediction = runModelInference(tflite, bitmap)
               errorMessage = ""

                thread {
                    runCatching {
                        loadModelFile(context.assets, "model.tflite")?.let { buf ->
                            Interpreter(buf).use { trainer ->
                                val result = FederateLearning.runTraining(bitmap, trainer, context)

                                // 메인(UI) 스레드로 올려서 State 업데이트
                                Handler(Looper.getMainLooper()).post {
                                    prediction = buildString {
                                        append("Class ${result.predictedIndex}: ")
                                        append(SkinLabels[result.predictedIndex])
                                        append(" with probability ${"%.5f".format(result.confidence)}")
                                    }
                                }
                            }
                        } ?: Log.e("AiCheckDogSkin", "학습용 모델 로드 실패 (buf == null)")
                    }.onFailure {
                        Log.e("AiCheckDogSkin", "연합학습 중 오류", it)
                    }
                }
            } else if (bitmap == null) {
                errorMessage = "이미지가 로드되지 않았습니다."
            } else if (!isModelLoaded) {
                errorMessage = "모델이 아직 로드되지 않았습니다."
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

            if (prediction.isNotEmpty()) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(sliceString(prediction, 9))
                        }
                        append("이(가) 의심됩니다")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 20.sp
                )
                Text(
                    text = sliceProbability(prediction),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp),
                    fontSize = 16.sp
                )
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onNavigateAiHealth,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFBF9270),
                contentColor = Color(0xFFFFF2C2)
            ),
            modifier = Modifier
                .width(300.dp)
                .height(50.dp)
        ) {
            Text(
                text = "메뉴로 돌아가기",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

private val SkinLabels = listOf(
    "구진 플라크","비듬/각질/상피성잔고리","태선화/과다색소침착",
    "농포/여드름","미란/궤양","결절/종괴","무증상"
)