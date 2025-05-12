package com.example.mungnyang.uicomponents.aiCamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun AiCameraDogEye(
    onCaptureSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // 카메라 리소스 관리를 위한 상태
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    
    // 라이프사이클 관찰자 추가
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        cameraProvider?.unbindAll()
                        imageCapture = null
                        cameraProvider = null
                    } catch (e: Exception) {
                        Log.e("AiCameraDogEye", "카메라 리소스 해제 실패", e)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 카메라 설정
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val preview = Preview.Builder().build()
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                imageCapture = null
                cameraProvider = null
                executor.shutdown()
            } catch (e: Exception) {
                Log.e("AiCameraDogEye", "카메라 리소스 해제 실패", e)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFF2C2))
    ) {
        if (hasCameraPermission) { 
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(700.dp)
            ) {
                AndroidView(  
                    modifier = Modifier.matchParentSize(),
                    factory = { context ->
                        PreviewView(context).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                cameraProvider = provider
                                preview.setSurfaceProvider(previewView.surfaceProvider)

                                imageCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                    .build()

                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    Log.e("AiCameraDogEye", "카메라 바인딩 실패", e)
                                }
                            } catch (e: Exception) {
                                Log.e("AiCameraDogEye", "카메라 프로바이더 가져오기 실패", e)
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
                Text(
                    text = "초점에 맞춰 하단 촬영 버튼을 눌러주세요",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp)
                        .background(Color(0x80000000)),
                )
            }

            // 캡처 버튼
            Button(
                onClick = {
                    val imageCapture = imageCapture ?: return@Button
                    val photoFile = createImageFile(context)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val savedUri = output.savedUri ?: FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                // 크롭 파일 생성 224x224
                                val croppedFile = File(photoFile.parent, "cropped_${photoFile.name}")
                                cropCenterSquareTo224(photoFile, croppedFile)
                                val croppedUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    croppedFile
                                )
                                Handler(Looper.getMainLooper()).post {
                                    onCaptureSuccess(croppedUri.toString())
                                }
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Log.e("AiCameraDogEye", "사진 촬영 실패", exc)
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("사진 촬영")
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("카메라 권한이 필요합니다")
            }
        }
    }
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    ).apply {
        deleteOnExit() // 앱 종료 시 자동 삭제
    }
}

fun cropCenterSquareTo224(inputFile: File, outputFile: File) {
    // 1. Bitmap 로드
    val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)

    // 2. EXIF에서 회전 정보 읽기
    val exif = ExifInterface(inputFile.absolutePath)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val rotatedBitmap = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> bitmap.rotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> bitmap.rotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> bitmap.rotate(270f)
        else -> bitmap
    }

    // 3. 중앙 크롭
    val size = minOf(rotatedBitmap.width, rotatedBitmap.height)
    val x = (rotatedBitmap.width - size) / 2
    val y = (rotatedBitmap.height - size) / 2
    val squareBitmap = Bitmap.createBitmap(rotatedBitmap, x, y, size, size)
    val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 224, 224, true)
    outputFile.outputStream().use { out ->
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    if (rotatedBitmap != bitmap) bitmap.recycle()
    squareBitmap.recycle()
    resizedBitmap.recycle()
    rotatedBitmap.recycle()
}

// Bitmap 확장 함수
fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = android.graphics.Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}