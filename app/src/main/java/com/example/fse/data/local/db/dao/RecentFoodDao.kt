package com.example.fse.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fse.data.local.db.entity.RecentFoodEntity

@Dao
interface RecentFoodDao {
    @Query("SELECT * FROM recent_food ORDER BY addedAt DESC LIMIT 50")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<RecentFoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentFoodEntity)
}
