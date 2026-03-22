package com.example.fse.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food")
data class FoodEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val brandName: String?,
    val foodType: String,
)
