package com.example.fisinginfo.data.api

import com.example.fisinginfo.data.model.FishingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FishingApiService {
    @GET("1192136/fcstFishingv2/GetFcstFishingApiServicev2")
    suspend fun getFishingInfo(
        @Query("serviceKey", encoded = true) serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int = 150,
        @Query("type") type: String = "json",
        @Query("gubun") gubun: String = "갯바위"
    ): Response<FishingResponse>
}