package com.example.fse.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fse.data.local.db.entity.PeriodStartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodStartDao {

    @Query("SELECT * FROM period_start ORDER BY startDate DESC")
    fun observeAllDesc(): Flow<List<PeriodStartEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PeriodStartEntity): Long

    @Query("DELETE FROM period_start WHERE id = :id")
    suspend fun deleteById(id: Long)
}
