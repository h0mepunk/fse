package com.example.fse.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_meal_template_line",
    foreignKeys = [
        ForeignKey(
            entity = SavedMealTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class SavedMealTemplateLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val foodId: Long,
    val servingId: Long,
    val foodName: String,
    val brandName: String?,
    val servingDescription: String,
    val multiplier: Double,
    val lineCalories: Double,
    val lineProtein: Double,
    val lineCarbs: Double,
    val lineFat: Double
)
