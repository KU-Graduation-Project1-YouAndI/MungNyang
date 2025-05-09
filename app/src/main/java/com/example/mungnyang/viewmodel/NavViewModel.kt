package com.example.mungnyang.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class NavViewModel: ViewModel() {
    val ID = "qwer"
    val PW = "qwer"

    var userId: String? = null
    var userPw: String? = null

    var loginStatus = mutableStateOf(false)

    fun checkInfo(id: String, pw: String): Boolean {
        return id == ID && pw == PW
    }

    fun setUserInfo(id: String, pw: String) {
        userId = id
        userPw = pw
    }
}