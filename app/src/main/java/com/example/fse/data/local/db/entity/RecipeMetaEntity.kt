package com.example.fse.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipe_meta")
data class RecipeMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
