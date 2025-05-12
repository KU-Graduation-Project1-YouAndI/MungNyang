package com.example.mungnyang

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        // SDK 초기화: 앱 키 넣기
        //KakaoMapSdk.init(this, "8e166c18c13ad4ffbba16bb578167326")
    }
}