package com.example.mungnyang.uicomponents.main

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory

@Preview
@Composable
fun PetWalk() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val mapView = remember { MapView(context) }

    // 라이프사이클 관리
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        factory = { 
            mapView.apply {
                start(object : MapLifeCycleCallback() {
                    override fun onMapDestroy() = Unit
                    override fun onMapError(e: Exception?) {
                        Log.e("KakaoMap", "지도 로딩 실패", e)
                    }
                    override fun onMapResumed() = Unit
                }, object : KakaoMapReadyCallback() {
                    override fun onMapReady(map: KakaoMap) {
                        val seoul = LatLng.from(37.5665, 126.9780)
                        val cameraUpdate = CameraUpdateFactory.newCenterPosition(seoul)
                        map.moveCamera(cameraUpdate)
                    }
                })
            }
        },
        update = {}
    )
}