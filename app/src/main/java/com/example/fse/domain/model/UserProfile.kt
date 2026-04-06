package com.example.fse.domain.model

enum class UserSex {
    UNSPECIFIED,
    MALE,
    FEMALE
}

enum class ActivityLevel(val multiplier: Double, val labelRu: String) {
    SEDENTARY(1.2, "Минимальная (сидячая)"),
    LIGHT(1.375, "Лёгкая"),
    MODERATE(1.55, "Умеренная"),
    ACTIVE(1.725, "Высокая"),
    VERY_ACTIVE(1.9, "Очень высокая")
}

/** Цель для расчёта целевых калорий и БЖУ */
enum class NutritionGoal(val labelRu: String) {
    WEIGHT_LOSS("Похудение"),
    MAINTENANCE("Поддержание"),
    MUSCLE_GAIN("Набор массы"),
    RECOMPOSITION("Рекомпозиция")
}
