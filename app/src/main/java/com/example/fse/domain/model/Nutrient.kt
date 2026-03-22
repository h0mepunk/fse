package com.example.fse.domain.model

enum class Nutrient(
    val displayName: String,
    val unit: String,
    val dailyRecommended: Double  // RDA-based defaults (adult)
) {
    Calories("Calories", "kcal", 2000.0),
    Protein("Protein", "g", 50.0),
    Carbs("Carbs", "g", 300.0),
    Fat("Fat", "g", 65.0),
    Fiber("Fiber", "g", 25.0),
    Sodium("Sodium", "mg", 2300.0),
    Calcium("Calcium", "mg", 1000.0),
    Iron("Iron", "mg", 18.0),
    VitaminA("Vitamin A", "mcg", 900.0),
    VitaminC("Vitamin C", "mg", 90.0),
    VitaminD("Vitamin D", "mcg", 20.0),
    Potassium("Potassium", "mg", 2600.0)
}
