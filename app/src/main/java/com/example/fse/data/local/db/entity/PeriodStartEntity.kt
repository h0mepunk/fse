package com.example.fse.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "period_start",
    indices = [Index(value = ["startDate"], unique = true)]
)
data class PeriodStartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO-8601 date (yyyy-MM-dd) */
    val startDate: String
)
