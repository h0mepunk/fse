package com.example.fse.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fse.data.local.db.entity.FavoriteEntity

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE foodId = :foodId")
    suspend fun deleteByFoodId(foodId: Long)

    @Query("SELECT * FROM favorite ORDER BY foodId")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) > 0 FROM favorite WHERE foodId = :foodId")
    suspend fun contains(foodId: Long): Boolean
}
