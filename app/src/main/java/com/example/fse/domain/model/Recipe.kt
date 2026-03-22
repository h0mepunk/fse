package com.example.fse.domain.model

data class Recipe(
    val id: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val ingredients: List<String>,
    val types: List<String>
)
