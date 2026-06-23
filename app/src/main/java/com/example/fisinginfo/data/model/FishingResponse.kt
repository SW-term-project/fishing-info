package com.example.fisinginfo.data.model

import com.google.gson.annotations.SerializedName

data class FishingResponse(
    @SerializedName("header")
    val header: FishingHeader,

    @SerializedName("body")
    val body: FishingBody?
)

data class FishingHeader(
    @SerializedName("resultCode")
    val resultCode: String,

    @SerializedName("resultMsg")
    val resultMsg: String
)

data class FishingBody(
    @SerializedName("items")
    val items: FishingItems?,

    @SerializedName("pageNo")
    val pageNo: Int,

    @SerializedName("numOfRows")
    val numOfRows: Int,

    @SerializedName("totalCount")
    val totalCount: Int
)

data class FishingItems(
    @SerializedName("item")
    val item: List<FishingItem>
)

data class FishingItem(
    @SerializedName("seafsPstnNm") val seafsPstnNm: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lot") val lot: Double,
    @SerializedName("seafsTgfshNm") val seafsTgfshNm: String?,
    @SerializedName("tdlvHrCn") val tdlvHrCn: String?,
    @SerializedName("totalIndex") val totalIndex: String?
)