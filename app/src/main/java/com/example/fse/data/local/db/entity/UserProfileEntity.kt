package com.example.fse.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val sex: String = "UNSPECIFIED",
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val ageYears: Int? = null,
    val activityMultiplier: Float = 1.55f,
    val goal: String = "MAINTENANCE"
)
