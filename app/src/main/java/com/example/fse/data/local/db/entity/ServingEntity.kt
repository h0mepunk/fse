package com.example.fse.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "serving",
    foreignKeys = [ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("foodId")]
)
data class ServingEntity(
    @PrimaryKey val id: Long,
    val foodId: Long,
    val description: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sodium: Double,
    val calcium: Double,
    val iron: Double,
    val vitaminA: Double,
    val vitaminC: Double,
    val vitaminD: Double,
    val potassium: Double,
)
