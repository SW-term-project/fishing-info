package com.example.fisinginfo.ui.detail

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fisinginfo.R
import com.example.fisinginfo.data.api.WeatherRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {
    private val WEATHER_API_KEY = "cfab04ed1ab8cd0d45900a3b08e82761"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // 1. 메인 화면(마커 클릭)에서 보낸 데이터 꺼내기
        val placeName = intent.getStringExtra("PLACE_NAME") ?: "알 수 없는 포인트"
        val tide = intent.getStringExtra("TIDE") ?: "정보 없음"
        val targetFish = intent.getStringExtra("TARGET_FISH") ?: "정보 없음"
        val totalIndex = intent.getStringExtra("TOTAL_INDEX") ?: "정보 없음"

        val lat = intent.getDoubleExtra("LAT", 0.0)
        val lon = intent.getDoubleExtra("LOT", 0.0)

        // 2. 화면(XML)에 있는 TextView 찾아서 글자 세팅하기
        findViewById<TextView>(R.id.tv_place_name).text = placeName
        findViewById<TextView>(R.id.tv_target_fish).text = targetFish
        findViewById<TextView>(R.id.tv_total_index).text = totalIndex
        findViewById<TextView>(R.id.tv_tide).text = tide

        // 3. 위경도가 0.0이 아니라 정상적으로 넘어왔다면, 여기서 날씨 API 함수를 실행
        if (lat != 0.0 && lon != 0.0) {
            fetchWeatherData(lat, lon)
        } else {
            Log.e("WEATHER_ERROR", "위도/경도 데이터를 받지 못했습니다.")
            findViewById<TextView>(R.id.tv_weather_status).text = "위치 정보 오류"
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            // 현재 화면(DetailActivity)을 종료하고 이전 화면(메인 지도)으로 돌아감
            finish()
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = WeatherRetrofitClient.apiService.getCurrentWeather(
                    lat = lat,
                    lon = lon,
                    apiKey = WEATHER_API_KEY
                )

                if (response.isSuccessful) {
                    val weatherBody = response.body()

                    withContext(Dispatchers.Main) {
                        weatherBody?.let {
                            // 4. 기온(소수점 버림)과 습도, 날씨 상태 추출
                            val currentTemp = "${it.main.temp.toInt()}°C"
                            val humidity = "습도: ${it.main.humidity}%"
                            val weatherDesc = it.weather.firstOrNull()?.desc ?: "정보 없음"

                            // 5. 파란색 카드 UI에 날씨 정보 업데이트
                            findViewById<TextView>(R.id.tv_current_temp).text = currentTemp
                            findViewById<TextView>(R.id.tv_humidity).text = humidity
                            findViewById<TextView>(R.id.tv_weather_status).text = weatherDesc

                            Log.d("WEATHER_SUCCESS", "기온: $currentTemp, 습도: $humidity, 상태: $weatherDesc")
                        }
                    }
                } else {
                    Log.e("WEATHER_ERROR", "날씨 응답 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("WEATHER_CRASH", "날씨 통신 오류: ${e.message}")
            }
        }
    }
}