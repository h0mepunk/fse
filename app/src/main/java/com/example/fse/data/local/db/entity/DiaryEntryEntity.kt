package com.example.fse.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "diary_entry",
    foreignKeys = [
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ServingEntity::class, parentColumns = ["id"], childColumns = ["servingId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("date"), Index("foodId"), Index("servingId")]
)
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val mealType: String,
    val foodId: Long,
    val servingId: Long,
    val multiplier: Double,
)
