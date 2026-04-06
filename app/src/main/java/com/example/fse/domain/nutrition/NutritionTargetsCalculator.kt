package com.example.fse.domain.nutrition

import com.example.fse.domain.model.ActivityLevel
import com.example.fse.domain.model.NutritionGoal
import com.example.fse.domain.model.UserSex

/**
 * Mifflin–St Jeor BMR, TDEE и целевые макросы (упрощённая схема).
 */
object NutritionTargetsCalculator {

    data class Result(
        val calories: Double,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double
    )

    fun calculate(
        sex: UserSex,
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        activity: ActivityLevel,
        goal: NutritionGoal
    ): Result {
        require(weightKg > 0 && heightCm > 0 && ageYears > 0) {
            "Нужны положительные вес, рост и возраст"
        }
        val bmr = when (sex) {
            UserSex.MALE -> 10 * weightKg + 6.25 * heightCm - 5 * ageYears + 5
            UserSex.FEMALE -> 10 * weightKg + 6.25 * heightCm - 5 * ageYears - 161
            UserSex.UNSPECIFIED -> {
                val m = 10 * weightKg + 6.25 * heightCm - 5 * ageYears + 5
                val f = 10 * weightKg + 6.25 * heightCm - 5 * ageYears - 161
                (m + f) / 2
            }
        }
        val tdee = bmr * activity.multiplier
        val kcalFactor = when (goal) {
            NutritionGoal.WEIGHT_LOSS -> 0.82
            NutritionGoal.MAINTENANCE -> 1.0
            NutritionGoal.MUSCLE_GAIN -> 1.10
            NutritionGoal.RECOMPOSITION -> 0.95
        }
        val targetKcal = (tdee * kcalFactor).coerceAtLeast(800.0)

        val proteinPerKg = when (goal) {
            NutritionGoal.WEIGHT_LOSS -> 2.0
            NutritionGoal.MAINTENANCE -> 1.6
            NutritionGoal.MUSCLE_GAIN -> 2.2
            NutritionGoal.RECOMPOSITION -> 2.0
        }
        val proteinG = (weightKg * proteinPerKg).coerceAtLeast(50.0)
        val fatKcal = targetKcal * 0.28
        val fatG = (fatKcal / 9.0).coerceAtLeast(30.0)
        val carbKcal = (targetKcal - proteinG * 4 - fatG * 9).coerceAtLeast(0.0)
        val carbsG = (carbKcal / 4.0).coerceAtLeast(80.0)

        return Result(
            calories = targetKcal,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG
        )
    }
}
