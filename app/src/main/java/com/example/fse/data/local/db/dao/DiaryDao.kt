package com.example.fse.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fse.data.local.db.entity.DiaryEntryEntity
import java.time.LocalDate

@Dao
interface DiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntryEntity)

    @Query("DELETE FROM diary_entry WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM diary_entry WHERE date = :date ORDER BY mealType, id")
    fun entriesForDate(date: LocalDate): kotlinx.coroutines.flow.Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entry ORDER BY date DESC, mealType, id")
    fun allEntries(): kotlinx.coroutines.flow.Flow<List<DiaryEntryEntity>>
}
