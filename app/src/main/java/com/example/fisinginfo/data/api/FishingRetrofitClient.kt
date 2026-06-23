package com.example.fisinginfo.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FishingRetrofitClient {
    private const val BASE_URL = "https://apis.data.go.kr/"

    val apiService: FishingApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FishingApiService::class.java)
    }
}