package com.example.fisinginfo.ui.identify

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.fisinginfo.R

class IdentifyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identify)

        // 우상단 뒤로가기 버튼
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }
}

