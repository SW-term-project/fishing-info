package com.example.fisinginfo.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FishingRetrofitClient {
    private const val BASE_URL = "https://apis.data.go.kr/"

    val apiService: FishingApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON을 Kotlin 객체로 자동 변환
            .build()
            .create(FishingApiService::class.java)
    }
}