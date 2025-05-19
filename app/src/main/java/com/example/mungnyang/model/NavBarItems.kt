package com.example.mungnyang.model

import com.example.mungnyang.R

object NavBarItems {
    val BarItems = listOf(
        BarItem(
            title = "AI 건강검진",
            onSelectIcon = R.drawable.health_on,
            unSelectIcon = R.drawable.health_off,
            routes = listOf("AiHealth", "AiCheckDogEye", "AiCheckDogSkin")
        ),
        BarItem(
            title = "펫 캘린더",
            onSelectIcon = R.drawable.calendar_on,
            unSelectIcon = R.drawable.calendar_off,
            routes = listOf("PetCalendar")
        ),
        BarItem(
            title = "펫 워크",
            onSelectIcon = R.drawable.walk_on,
            unSelectIcon = R.drawable.walk_off,
            routes = listOf("PetWalk")
        ),
        BarItem(
            title = "멍냥 일지",
            onSelectIcon = R.drawable.log_on,
            unSelectIcon = R.drawable.log_off,
            routes = listOf("PetLog")
        )
    )
}