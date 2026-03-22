package com.example.fse.domain.model

/**
 * A saved meal template from FatSecret (e.g. "Power Snack" for Lunch/Other).
 */
data class SavedMeal(
    val id: Long,
    val name: String,
    val description: String?,
    val mealTypes: List<String>,  // e.g. ["Lunch", "Other"]
    val items: List<SavedMealItem>
)

/**
 * A food item within a saved meal.
 */
data class SavedMealItem(
    val id: Long,
    val foodId: Long,
    val name: String,
    val servingId: Long,
    val numberOfUnits: Double
)
