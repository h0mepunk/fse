package com.example.fse.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_meal_template")
data class SavedMealTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
