package com.example.fse.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "norm")
data class NormEntity(
    @PrimaryKey val nutrient: String,
    val value: Double,
)
