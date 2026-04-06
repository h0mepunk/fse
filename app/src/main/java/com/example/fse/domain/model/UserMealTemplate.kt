package com.example.fse.domain.model

import java.time.LocalDate

/**
 * Locally stored meal template (e.g. saved from diary) — not FatSecret saved_meal API.
 */
data class UserMealTemplate(
    val id: Long,
    val name: String,
    val lines: List<UserMealTemplateLine>
) {
    val totalCalories: Double get() = lines.sumOf { it.lineCalories }
    val totalProtein: Double get() = lines.sumOf { it.lineProtein }
    val totalCarbs: Double get() = lines.sumOf { it.lineCarbs }
    val totalFat: Double get() = lines.sumOf { it.lineFat }
}

data class UserMealTemplateLine(
    val id: Long,
    val templateId: Long,
    val foodId: Long,
    val servingId: Long,
    val foodName: String,
    val brandName: String?,
    val servingDescription: String,
    val multiplier: Double,
    val lineCalories: Double,
    val lineProtein: Double,
    val lineCarbs: Double,
    val lineFat: Double
)

/** Passed when navigating from diary after long-press save. */
data class PendingMealTemplateFromDiary(
    val date: LocalDate,
    val mealType: MealType,
    val entries: List<DiaryEntry>
)
