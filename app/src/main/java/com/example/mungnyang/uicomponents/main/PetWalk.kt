package com.example.mungnyang.uicomponents.main

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.compose.rememberFusedLocationSource

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

    val granted = permissionsState.permissions.any { it.status.isGranted }

    val cameraPositionState = rememberCameraPositionState()
    val locationSource = rememberFusedLocationSource()

    if (granted) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            locationSource = locationSource,
            properties = MapProperties(
                locationTrackingMode = LocationTrackingMode.Face
            ),
            uiSettings = MapUiSettings(
                isLocationButtonEnabled = true
            )
        )
    } else {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        )
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