package com.example.fse.domain.model

/**
 * A saved meal from the FatSecret profile (saved_meals.get), with lines from saved_meal_items.get.
 */
data class ProfileSavedMeal(
    val id: String,
    val name: String,
    val description: String?,
    val suitableMeals: String?,
    val items: List<ProfileSavedMealItem>
)

data class ProfileSavedMealItem(
    val itemId: String,
    val foodId: Long,
    val name: String,
    val servingId: Long,
    val numberOfUnits: Double
)
