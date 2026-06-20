package com.example.fisinginfo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// 간단한 로컬 DB 엔티티: 어종 이름을 PK로 함
@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey val speciesName: String,
    val avgLength: String?,
    val avgWeight: String?,
    val depth: String?,
    // 계절은 쉼표(,)로 구분된 문자열로 저장 (예: "spring,summer")
    val bestSeasons: String?,
    // 추천 채비법은 마크다운/텍스트 형태로 저장
    val fishingMethods: String?,
    val imageUrl: String?
)

