package com.example.mungnyang.uicomponents.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mungnyang.model.WalkRecord
import com.example.mungnyang.viewmodel.WalkRecordViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PetLog(
    viewModel: WalkRecordViewModel,
    modifier: Modifier = Modifier
) {
    val walkRecords by viewModel.walkRecords.collectAsState()
    
    // 샘플 데이터 추가 (테스트용)
    LaunchedEffect(Unit) {
//        viewModel.addSampleData()
        Log.i("ddd", walkRecords.toString())
    }

    Column(
        modifier = modifier
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "산책 기록",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        
        if (walkRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // 남은 공간을 모두 차지
                contentAlignment = Alignment.Center
            ) {
                Text("아직 산책 기록이 없어요. 산책을 시작해보세요!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // 남은 공간을 모두 차지
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(walkRecords) { record ->
                    WalkRecordItem(record = record)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalkRecordItem(record: WalkRecord) {
    val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분", Locale.getDefault())
    val durationMinutes = record.duration / (60 * 1000)
    val distanceKm = String.format("%.2f", record.distance / 1000)
    
    Card(
        onClick = { /* 클릭 이벤트 처리 */ },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(record.date),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${durationMinutes}분",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${distanceKm}km",
                fontSize = 20.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        }
    }
}