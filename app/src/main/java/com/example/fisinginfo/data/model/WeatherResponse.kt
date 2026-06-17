package com.example.fisinginfo.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("main") val main: MainData,
    @SerializedName("weather") val weather: List<WeatherData>,
    @SerializedName("wind") val wind: WindData
)

data class MainData(
    @SerializedName("temp") val temp: Double,       // 실시간 기온
    @SerializedName("humidity") val humidity: Int   // 습도 (%)
)

data class WeatherData(
    @SerializedName("main") val main: String,         // 날씨 상태 (예: Clouds, Rain)
    @SerializedName("description") val desc: String,  // 날씨 설명 (예: 튼구름)
    @SerializedName("icon") val icon: String          // 날씨 아이콘 ID (예: 02d)
)

data class WindData(
    @SerializedName("speed") val speed: Double       // 풍속 (m/s)
)