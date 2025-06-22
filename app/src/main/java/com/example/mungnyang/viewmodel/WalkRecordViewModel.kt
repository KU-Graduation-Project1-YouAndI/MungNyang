package com.example.mungnyang.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mungnyang.model.WalkRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class WalkRecordViewModel : ViewModel() {
    private val _walkRecords = MutableStateFlow<List<WalkRecord>>(emptyList())
    val walkRecords: StateFlow<List<WalkRecord>> = _walkRecords.asStateFlow()

    fun addWalkRecord(record: WalkRecord) {
        viewModelScope.launch {
            if (_walkRecords.value.isNotEmpty()) {
                Log.i("ddd", "이미 데이터가 있어서 샘플 데이터를 추가하지 않습니다.")
                return@launch
            }
            _walkRecords.update { it + record }
            Log.i("ddd", walkRecords.toString())
        }
    }

    // 샘플 데이터 제거 또는 조건부로 추가
    fun addSampleData() {
        viewModelScope.launch {
            // 이미 데이터가 있으면 샘플 데이터를 추가하지 않음
            if (_walkRecords.value.isNotEmpty()) return@launch

            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L

            val sampleData = listOf(
                WalkRecord(1200.0, 1800000, Date(now - 6 * oneDay)),
                WalkRecord(1500.0, 2100000, Date(now - 5 * oneDay)),
                WalkRecord(800.0, 1200000, Date(now - 4 * oneDay)),
                WalkRecord(2000.0, 2700000, Date(now - 3 * oneDay)),
                WalkRecord(1700.0, 2400000, Date(now - 2 * oneDay)),
                WalkRecord(1300.0, 1800000, Date(now - oneDay)),
                WalkRecord(1600.0, 2100000, Date(now))
            )

            _walkRecords.value = sampleData.sortedByDescending { it.date }
        }
    }

    // 모든 기록 삭제 (테스트용)
    fun clearAllRecords() {
        viewModelScope.launch {
            _walkRecords.value = emptyList()
        }
    }

    // 최신 기록 가져오기
    fun getLatestRecord(): WalkRecord? {
        return _walkRecords.value.firstOrNull()
    }
}