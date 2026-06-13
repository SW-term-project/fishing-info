package com.example.fisinginfo.ui.detail

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fisinginfo.R

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 아래에서 만들 activity_detail.xml 화면을 연결
        setContentView(R.layout.activity_detail)

        // 1. 메인 화면에서 보낸 데이터 꺼내기
        val placeName = intent.getStringExtra("PLACE_NAME") ?: "알 수 없는 포인트"
        val tide = intent.getStringExtra("TIDE") ?: "정보 없음"
        val targetFish = intent.getStringExtra("TARGET_FISH") ?: "정보 없음"
        val totalIndex = intent.getStringExtra("TOTAL_INDEX") ?: "정보 없음"

        // 2. 화면(XML)에 있는 TextView 4개를 찾아서 글자 세팅하기
        findViewById<TextView>(R.id.tv_place_name).text = placeName
        findViewById<TextView>(R.id.tv_tide).text = "물때: $tide"
        findViewById<TextView>(R.id.tv_target_fish).text = "대상어종: $targetFish"
        findViewById<TextView>(R.id.tv_total_index).text = "바다낚시지수: $totalIndex"
    }
}