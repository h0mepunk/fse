package com.example.fse.domain.model

import java.time.LocalDate

data class DiaryEntry(
    val id: String,
    val date: LocalDate,
    val mealType: MealType,
    val food: Food,
    val serving: Serving,
    val multiplier: Double = 1.0  // e.g. 2.0 = 2 servings
)

enum class MealType(val displayName: String) {
    Breakfast("Breakfast"),
    Lunch("Lunch"),
    Dinner("Dinner"),
    Snack("Snack"),
    Other("Other")
}
