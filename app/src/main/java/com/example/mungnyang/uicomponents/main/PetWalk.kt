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
import com.example.mungnyang.R
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
fun PetWalk() {
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
                @Suppress("MissingPermission") // 권한 체크는 allPermissionsGranted로 이미 수행됨
                val location = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    currentUserLocation = currentLatLng
                    cameraPositionState.position = CameraPosition(currentLatLng, 15.0) // 줌 레벨 15로 카메라 이동
                }
            } catch (e: SecurityException) {
                // 권한 관련 예외 처리 (예: 사용자에게 알림)
            } catch (e: Exception) {
                // 기타 예외 처리
            }
        }
    }

    // 위치 추적 및 경로, 거리 계산 (currentUserLocation 업데이트 로직은 동일)
    LaunchedEffect(isWalking, allPermissionsGranted) {
        if (isWalking && allPermissionsGranted) {
            hasWalkedOnce = true
            startTime = System.currentTimeMillis()
            totalDistance = 0.0

            try {
                @Suppress("MissingPermission")
                val initialLocation = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                initialLocation?.let {
                    val initialLatLng = LatLng(it.latitude, it.longitude)
                    pathCoords = listOf(initialLatLng)
                    currentUserLocation = initialLatLng // currentUserLocation 업데이트
                    markerState.position = initialLatLng // 마커 상태의 position 직접 업데이트
                }
            } catch (e: Exception) {
                // ...
            }

            while (isWalking) {
                try {
                    @Suppress("MissingPermission")
                    val location = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    location?.let {
                        val newLatLng = LatLng(it.latitude, it.longitude)
                        currentUserLocation = newLatLng // currentUserLocation 업데이트
                        markerState.position = newLatLng // 마커 상태의 position 직접 업데이트 <--- 중요!
                        if (pathCoords.isNotEmpty()) {
                            val prev = pathCoords.last()
                            if (prev != newLatLng) {
                                totalDistance += calculateDistance(prev, newLatLng)
                                pathCoords = pathCoords + newLatLng
                            }
                        } else {
                            pathCoords = listOf(newLatLng)
                        }
                    }
                } catch (e: Exception) {
                    // ...
                }
                delay(1000)
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
            Spacer(modifier = Modifier.height(60.dp)) // 상단 여백

            if (!isWalking && pathCoords.isNotEmpty() && elapsedTime > 0) {
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

                        Text("시간")

                        Image(
                            painter = painterResource(id = R.drawable.shiba),
                            contentDescription = null,
                            modifier = Modifier.width(70.dp).height(70.dp)
                        )

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
                                    text = "${"%.2f".format(totalDistance)}",
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
                                val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
                                val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
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
                                    text = "분",
                                    fontSize = 22.sp,
                                    color = Color(0xFFE3B7A0)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (allPermissionsGranted) {
                                    isWalking = true
                                    // 산책 시작 시, 경로 및 시간/거리 초기화 (필요하다면 LaunchedEffect에서 이미 처리)
                                    // pathCoords = emptyList()
                                    // totalDistance = 0.0
                                    // elapsedTime = 0L
                                    // startTime = System.currentTimeMillis() // LaunchedEffect에서 처리
                                } else {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                            },
                            enabled = allPermissionsGranted, // 권한이 있을 때만 활성화
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

            // isWalking 상태에 따라 NaverMap의 높이를 동적으로 설정
            val mapHeight =
                if (isWalking || !hasWalkedOnce) { 500.dp }
                else { 250.dp }

            if (allPermissionsGranted) {
                // NaverMap Composable
                NaverMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight) // 지도의 높이 설정
                        .clip(RoundedCornerShape(16.dp))
                        .border(width = 1.dp, color = Color.Gray, shape = RoundedCornerShape(16.dp)), // 테두리 추가
                    cameraPositionState = cameraPositionState, // 카메라 위치 상태
                    locationSource = locationSource, // FusedLocationSource 설정
                    properties = MapProperties(
                        locationTrackingMode = LocationTrackingMode.Face,
                        maxZoom = 18.0,
                        minZoom = 5.0
                    ),
                    uiSettings = MapUiSettings(
                        isLocationButtonEnabled = true, // 현재 위치 버튼 활성화
                        isCompassEnabled = true,
                        isZoomControlEnabled = true,
                        isScrollGesturesEnabled = true,
                        isZoomGesturesEnabled = true,
                        isTiltGesturesEnabled = true,
                        isRotateGesturesEnabled = true,
                        isStopGesturesEnabled = true,
                        logoGravity = Gravity.BOTTOM or Gravity.START // 로고 위치 변경 (예시)
                    )
                ) {
                    // 현재 사용자 위치에 마커 표시
                    currentUserLocation?.let { position ->
                        Marker(
                            state = markerState,
                            icon = OverlayImage.fromResource(R.drawable.shiba),
                            width = 40.dp, // 마커 너비
                            height = 50.dp, // 마커 높이
                        )
                    }

                    // 산책 경로 표시 (pathCoords가 두 개 이상의 좌표를 가질 때)
                    if (pathCoords.size >= 2) {
                        PolylineOverlay(
                            coords = pathCoords,
                            width = 5.dp, // 경로 선의 두께
                            color = Color(0xFFBF9270) // 경로 선의 색상
                        )
                    }
                }
            } else {
                // 권한이 없을 때 보여줄 화면 또는 메시지
                Text(
                    text = "위치 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // 지도와 버튼 사이 여백

            // 산책 중 UI와 시작 버튼 UI를 isWalking 상태에 따라 분기
            if (isWalking) {
                // 산책 중일 때: 거리, 중지 버튼, 시간 표시
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 총 거리
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${"%.2f".format(totalDistance)}m",
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
                                    showStopWalkingDialog = true // 중지 확인 다이얼로그 표시
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

                    // 소요 시간
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60 // 분을 제외한 나머지 초
                    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$timeFormatted",
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
            } else if (!hasWalkedOnce) {
                // 산책 시작 전일 때: 산책 시작 버튼 표시
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            if (allPermissionsGranted) {
                                isWalking = true
                                // 산책 시작 시, 경로 및 시간/거리 초기화 (필요하다면 LaunchedEffect에서 이미 처리)
                                // pathCoords = emptyList()
                                // totalDistance = 0.0
                                // elapsedTime = 0L
                                // startTime = System.currentTimeMillis() // LaunchedEffect에서 처리
                            } else {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        },
                        enabled = allPermissionsGranted, // 권한이 있을 때만 활성화
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
                        showStopWalkingDialog = false // 다이얼로그 밖을 클릭해도 닫히도록
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
                                isWalking = false
                                showStopWalkingDialog = false
                                // 필요하다면 여기서 산책 데이터 저장 등의 추가 로직 수행
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

//@Preview
//@Composable
//fun PetWalk() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    val mapView = remember { MapView(context) }
//
//    // 라이프사이클 관리
//    DisposableEffect(lifecycleOwner) {
//        val observer = LifecycleEventObserver { _, event ->
//            when (event) {
//                Lifecycle.Event.ON_RESUME -> mapView.resume()
//                Lifecycle.Event.ON_PAUSE -> mapView.pause()
//                else -> {}
//            }
//        }
//        lifecycleOwner.lifecycle.addObserver(observer)
//        onDispose {
//            lifecycleOwner.lifecycle.removeObserver(observer)
//            mapView.finish()
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.White)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            Spacer(modifier = Modifier.height(60.dp))
//
//            AndroidView(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(500.dp)
//                    .clip(RoundedCornerShape(16.dp))
//                    .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(16.dp)),
//                factory = {
//                    mapView.apply {
//                        start(object : MapLifeCycleCallback() {
//                            override fun onMapDestroy() = Unit
//                            override fun onMapError(e: Exception?) {
//                                Log.e("KakaoMap", "지도 로딩 실패", e)
//                            }
//
//                            override fun onMapResumed() = Unit
//                        }, object : KakaoMapReadyCallback() {
//                            override fun onMapReady(map: KakaoMap) {
//                                val seoul = LatLng.from(37.5665, 126.9780)
//                                val cameraUpdate = CameraUpdateFactory.newCenterPosition(seoul)
//                                map.moveCamera(cameraUpdate)
//                            }
//                        })
//                    }
//                },
//                update = {}
//            )
//        }
//    }
//}