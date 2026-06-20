package com.example.fisinginfo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SpeciesDao {
    @Query("SELECT * FROM species WHERE speciesName = :name LIMIT 1")
    suspend fun getSpeciesByName(name: String): SpeciesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(species: SpeciesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SpeciesEntity>)
}

