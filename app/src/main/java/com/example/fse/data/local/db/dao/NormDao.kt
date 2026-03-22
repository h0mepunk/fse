package com.example.fse.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fse.data.local.db.entity.NormEntity

@Dao
interface NormDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(norm: NormEntity)

    @Query("SELECT * FROM norm WHERE nutrient = :nutrient")
    fun get(nutrient: String): kotlinx.coroutines.flow.Flow<NormEntity?>

    @Query("SELECT * FROM norm WHERE value > 0")
    fun getAllCustom(): kotlinx.coroutines.flow.Flow<List<NormEntity>>

    @Query("DELETE FROM norm WHERE nutrient = :nutrient")
    suspend fun delete(nutrient: String)

    @Query("DELETE FROM norm")
    suspend fun deleteAll()
}
