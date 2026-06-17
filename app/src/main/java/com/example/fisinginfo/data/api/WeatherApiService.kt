package com.example.fisinginfo.data.api

import com.example.fisinginfo.data.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric", // 섭씨 온도를 받기 위해 필수!
        @Query("lang") lang: String = "kr"        // 날씨 설명을 한글로 받기 위해 설정
    ): Response<WeatherResponse>
}