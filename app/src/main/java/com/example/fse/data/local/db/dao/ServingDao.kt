package com.example.fse.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fse.data.local.db.entity.ServingEntity

@Dao
interface ServingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(serving: ServingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(servings: List<ServingEntity>)

    @Query("SELECT * FROM serving WHERE id = :id AND foodId = :foodId")
    suspend fun getById(id: Long, foodId: Long): ServingEntity?

    @Query("SELECT * FROM serving WHERE foodId = :foodId")
    suspend fun getByFoodId(foodId: Long): List<ServingEntity>

    @Query("SELECT * FROM serving WHERE foodId IN (:foodIds)")
    suspend fun getByFoodIds(foodIds: List<Long>): List<ServingEntity>
}
