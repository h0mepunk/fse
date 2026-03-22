package com.example.fse.data.local

import com.example.fse.domain.model.DiaryEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LocalDiaryStore {
    fun allEntries(): Flow<List<DiaryEntry>>
    fun entriesForDate(date: LocalDate): Flow<List<DiaryEntry>>
    suspend fun add(entry: DiaryEntry)
    suspend fun remove(entryId: String)
}
