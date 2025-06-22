package com.example.mungnyang.model

import java.util.Date

data class WalkRecord(
    val distance: Double, // 미터 단위
    val duration: Long, // 밀리초 단위
    val date: Date = Date()
)
