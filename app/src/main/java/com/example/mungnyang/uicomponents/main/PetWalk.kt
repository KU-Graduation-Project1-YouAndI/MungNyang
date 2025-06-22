package com.example.mungnyang.uicomponents.main

import android.Manifest
import android.location.Location
import android.view.Gravity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mungnyang.R
import com.example.mungnyang.model.WalkRecord
import com.example.mungnyang.viewmodel.WalkRecordViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.PolylineOverlay
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.compose.rememberFusedLocationSource
import com.naver.maps.map.compose.rememberMarkerState
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// 거리 계산 함수
fun calculateDistance(start: LatLng, end: LatLng): Double {
    val startLoc = Location("").apply {
        latitude = start.latitude
        longitude = start.longitude
    }
    val endLoc = Location("").apply {
        latitude = end.latitude
        longitude = end.longitude
    }
    return startLoc.distanceTo(endLoc).toDouble()
}

@OptIn(ExperimentalNaverMapApi::class, ExperimentalPermissionsApi::class)
@Composable
fun PetWalk(
    viewModel: WalkRecordViewModel = viewModel()
) {
    // ViewModel의 walkRecords 상태를 관찰
    val walkRecords by viewModel.walkRecords.collectAsState()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    val allPermissionsGranted = permissionsState.permissions.all { it.status.isGranted }

    val cameraPositionState = rememberCameraPositionState()
    val locationSource = rememberFusedLocationSource(isCompassEnabled = true)

    val context = LocalContext.current

    val fusedClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var isWalking by remember { mutableStateOf(false) }
    var pathCoords by remember { mutableStateOf(listOf<LatLng>()) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var startTime by remember { mutableStateOf(0L) }
    var elapsedTime by remember { mutableStateOf(0L) }

    // 마지막 산책 기록을 저장하는 상태 추가
    var lastWalkRecord by remember { mutableStateOf<WalkRecord?>(null) }

    // 마커 상태를 remember로 관리
    val markerState = rememberMarkerState()

    // 현재 사용자 위치를 저장할 상태
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) }

    // 산책 종료 확인 다이얼로그를 위한 상태
    var showStopWalkingDialog by remember { mutableStateOf(false) }

    // 산책을 한 번이라도 시작했었는지 추적하는 상태
    var hasWalkedOnce by remember { mutableStateOf(false) }

    // 권한이 부여되면 현재 위치를 가져오고 카메라 및 마커 위치를 업데이트하는 LaunchedEffect
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            try {
                @Suppress("MissingPermission")
                val location = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    currentUserLocation = currentLatLng
                    cameraPositionState.position = CameraPosition(currentLatLng, 15.0)
                }
            } catch (e: SecurityException) {
                // 권한 관련 예외 처리
            } catch (e: Exception) {
                // 기타 예외 처리
            }
        }
    }

    // 위치 추적 및 경로, 거리 계산
    LaunchedEffect(isWalking, allPermissionsGranted) {
        if (isWalking && allPermissionsGranted) {
            hasWalkedOnce = true
            startTime = System.currentTimeMillis()
            totalDistance = 0.0
            pathCoords = emptyList() // 경로 초기화

            try {
                @Suppress("MissingPermission")
                val initialLocation = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                initialLocation?.let {
                    val initialLatLng = LatLng(it.latitude, it.longitude)
                    pathCoords = listOf(initialLatLng)
                    currentUserLocation = initialLatLng
                    markerState.position = initialLatLng
                }
            } catch (e: Exception) {
                // 예외 처리
            }

            while (isWalking) {
                try {
                    @Suppress("MissingPermission")
                    val location = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    location?.let {
                        val newLatLng = LatLng(it.latitude, it.longitude)
                        currentUserLocation = newLatLng
                        markerState.position = newLatLng

                        if (pathCoords.isNotEmpty()) {
                            val prev = pathCoords.last()
                            val distance = calculateDistance(prev, newLatLng)
                            // 최소 이동 거리 체크 (5미터 이상일 때만 추가)
                            if (distance >= 5.0) {
                                totalDistance += distance
                                pathCoords = pathCoords + newLatLng
                            }
                        } else {
                            pathCoords = listOf(newLatLng)
                        }
                    }
                } catch (e: Exception) {
                    // 예외 처리
                }
                delay(2000) // 2초마다 위치 업데이트
                elapsedTime = System.currentTimeMillis() - startTime
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // 마지막 산책 기록 표시 (산책 중이 아니고, 기록이 있을 때)
            if (!isWalking && lastWalkRecord != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFFFFE1B3),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "🐾 마지막 산책 기록 🐾",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF854C22)
                        )

                        Text(
                            text = SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분", Locale.getDefault())
                                .format(Date(lastWalkRecord!!.date.time)),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Image(
                            painter = painterResource(id = R.drawable.shiba),
                            contentDescription = null,
                            modifier = Modifier.width(70.dp).height(70.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "산책 거리",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE3B7A0)
                                )
                                Text(
                                    text = "${"%.0f".format(lastWalkRecord!!.distance)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF854C22)
                                )
                                Text(
                                    text = "m",
                                    fontSize = 22.sp,
                                    color = Color(0xFFE3B7A0)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val minutes = TimeUnit.MILLISECONDS.toMinutes(lastWalkRecord!!.duration)
                                val seconds = TimeUnit.MILLISECONDS.toSeconds(lastWalkRecord!!.duration) % 60
                                val timeFormatted = String.format("%02d:%02d", minutes, seconds)

                                Text(
                                    text = "산책 시간",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE3B7A0)
                                )
                                Text(
                                    text = timeFormatted,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF854C22)
                                )
                                Text(
                                    text = "분:초",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE3B7A0)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (allPermissionsGranted) {
                                    isWalking = true
                                } else {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                            },
                            enabled = allPermissionsGranted,
                            modifier = Modifier
                                .width(200.dp)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF9270)),
                            shape = RoundedCornerShape(28.dp),
                        ) {
                            Text(
                                "산책 시작",
                                fontSize = 18.sp,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 지도 높이 설정
            val mapHeight = if (isWalking || lastWalkRecord == null) 500.dp else 250.dp

            if (allPermissionsGranted) {
                NaverMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .border(width = 1.dp, color = Color.Gray, shape = RoundedCornerShape(16.dp)),
                    cameraPositionState = cameraPositionState,
                    locationSource = locationSource,
                    properties = MapProperties(
                        locationTrackingMode = LocationTrackingMode.Face,
                        maxZoom = 18.0,
                        minZoom = 5.0
                    ),
                    uiSettings = MapUiSettings(
                        isLocationButtonEnabled = true,
                        isCompassEnabled = true,
                        isZoomControlEnabled = true,
                        isScrollGesturesEnabled = true,
                        isZoomGesturesEnabled = true,
                        isTiltGesturesEnabled = true,
                        isRotateGesturesEnabled = true,
                        isStopGesturesEnabled = true,
                        logoGravity = Gravity.BOTTOM or Gravity.START
                    )
                ) {
                    currentUserLocation?.let { position ->
                        Marker(
                            state = markerState,
                            icon = OverlayImage.fromResource(R.drawable.shiba),
                            width = 40.dp,
                            height = 50.dp,
                        )
                    }

                    if (pathCoords.size >= 2) {
                        PolylineOverlay(
                            coords = pathCoords,
                            width = 5.dp,
                            color = Color(0xFFBF9270)
                        )
                    }
                }
            } else {
                Text(
                    text = "위치 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // UI 분기 처리
            if (isWalking) {
                // 산책 중 UI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${"%.0f".format(totalDistance)}m",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF854C22)
                        )
                        Text(
                            text = "거리",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE3B7A0)
                        )
                    }

                    if (!showStopWalkingDialog) {
                        Image(
                            painter = painterResource(id = R.drawable.petwalk1),
                            contentDescription = null,
                            modifier = Modifier
                                .clickable {
                                    showStopWalkingDialog = true
                                }
                                .height(50.dp).width(50.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.petwalk2),
                            contentDescription = null,
                            modifier = Modifier.height(50.dp).width(50.dp)
                        )
                    }

                    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
                    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF854C22)
                        )
                        Text(
                            text = "시간",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE3B7A0)
                        )
                    }
                }
            } else if (lastWalkRecord == null) {
                // 첫 산책 시작 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            if (allPermissionsGranted) {
                                isWalking = true
                            } else {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        },
                        enabled = allPermissionsGranted,
                        modifier = Modifier
                            .width(200.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF2C2)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            "산책 시작",
                            fontSize = 18.sp,
                            color = Color(0xFFBF9270)
                        )
                    }
                }
            }

            // 산책 종료 확인 다이얼로그
            if (showStopWalkingDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showStopWalkingDialog = false
                    },
                    title = {
                        Image(
                            painter = painterResource(id = R.drawable.shiba),
                            contentDescription = null
                        )
                    },
                    text = {
                        Text("여기까지 산책을 종료할까멍?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // 산책 기록 생성 및 저장
                                val walkRecord = WalkRecord(
                                    distance = totalDistance,
                                    duration = System.currentTimeMillis() - startTime,
                                    date = Date(startTime)
                                )

                                // ViewModel에 저장
                                viewModel.addWalkRecord(walkRecord)

                                // 마지막 산책 기록 업데이트
                                lastWalkRecord = walkRecord

                                // 상태 초기화
                                isWalking = false
                                showStopWalkingDialog = false
                                pathCoords = emptyList()
                                totalDistance = 0.0
                                elapsedTime = 0L
                            }
                        ) {
                            Text("산책 종료다멍", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showStopWalkingDialog = false
                            }
                        ) {
                            Text("아니다멍")
                        }
                    },
                    containerColor = Color(0xFFFFF2C2),
                )
            }
        }
    }
}