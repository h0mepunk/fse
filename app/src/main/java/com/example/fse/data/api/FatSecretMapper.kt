package com.example.fse.data.api

import com.example.fse.domain.model.Food
import com.example.fse.domain.model.Serving
import com.example.fse.domain.model.MealType

fun FatSecretDto.FoodSearchItemV1.toDomain(): Food {
    val desc = food_description ?: "1 serving"
    val v = parseFoodDescription(desc)
    return Food(
        id = food_id,
        name = food_name,
        brandName = brand_name,
        foodType = food_type,
        servings = listOf(Serving(0, desc, v.calories, v.protein, v.carbs, v.fat, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    )
}

private fun parseFoodDescription(desc: String): ServingValues {
    val calRegex = Regex("""Calories:\s*(\d+\.?\d*)""", RegexOption.IGNORE_CASE)
    val pRegex = Regex("""Protein:\s*(\d+\.?\d*)g""", RegexOption.IGNORE_CASE)
    val cRegex = Regex("""Carb(ohydrate|s)?:\s*(\d+\.?\d*)g""", RegexOption.IGNORE_CASE)
    val fRegex = Regex("""Fat:\s*(\d+\.?\d*)g""", RegexOption.IGNORE_CASE)
    return ServingValues(
        calRegex.find(desc)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
        pRegex.find(desc)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
        cRegex.find(desc)?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0,
        fRegex.find(desc)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    )
}

private data class ServingValues(val calories: Double, val protein: Double, val carbs: Double, val fat: Double)

fun FatSecretDto.FoodSearchItem.toDomain(): Food {
    val servings = servingList(FatSecretClient.json)
    return Food(
        id = food_id,
        name = food_name,
        brandName = brand_name,
        foodType = food_type,
        servings = if (servings.isEmpty()) {
            listOf(Serving(0, "1 serving", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        } else {
            servings.map { it.toDomain() }
        }
    )
}

fun FatSecretDto.FoodDetailDto.toDomain(json: kotlinx.serialization.json.Json): Food {
    val servings = servingList(json)
    return Food(
        id = food_id,
        name = food_name,
        brandName = brand_name,
        foodType = food_type,
        servings = if (servings.isEmpty()) {
            listOf(Serving(0, "1 serving", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        } else {
            servings.map { it.toDomain() }
        }
    )
}

fun FatSecretProfileDto.ProfileFoodItem.toDomain(): Food {
    val s = Serving(
        id = serving_id.toLongOrNull() ?: 0L,
        description = food_description ?: "1 serving",
        calories = 0.0,
        protein = 0.0,
        carbs = 0.0,
        fat = 0.0,
        fiber = 0.0,
        sodium = 0.0,
        calcium = 0.0,
        iron = 0.0,
        vitaminA = 0.0,
        vitaminC = 0.0,
        vitaminD = 0.0,
        potassium = 0.0
    )
    return Food(
        id = food_id.toLongOrNull() ?: 0L,
        name = food_name,
        brandName = null,
        foodType = food_type,
        servings = listOf(s)
    )
}

fun FatSecretProfileDto.FoodEntryItem.toDiaryEntry(): com.example.fse.domain.model.DiaryEntry {
    val food = FatSecretProfileDto.ProfileFoodItem(
        food_id = food_id,
        food_name = food_entry_name,
        food_type = "Generic",
        serving_id = serving_id,
        number_of_units = number_of_units
    ).toDomain()
    val serving = com.example.fse.domain.model.Serving(
        id = serving_id.toLongOrNull() ?: 0L,
        description = food_entry_description,
        calories = calories?.toDoubleOrNull() ?: 0.0,
        protein = protein?.toDoubleOrNull() ?: 0.0,
        carbs = carbohydrate?.toDoubleOrNull() ?: 0.0,
        fat = fat?.toDoubleOrNull() ?: 0.0,
        fiber = 0.0,
        sodium = 0.0,
        calcium = 0.0,
        iron = 0.0,
        vitaminA = 0.0,
        vitaminC = 0.0,
        vitaminD = 0.0,
        potassium = 0.0
    )
    val mealType = when (meal.lowercase()) {
        "breakfast" -> MealType.Breakfast
        "lunch" -> MealType.Lunch
        "dinner" -> MealType.Dinner
        "snack" -> MealType.Snack
        else -> MealType.Other
    }
    val date = java.time.LocalDate.EPOCH.plusDays(date_int.toLongOrNull() ?: 0L)
    return com.example.fse.domain.model.DiaryEntry(
        id = food_entry_id,
        date = date,
        mealType = mealType,
        food = food,
        serving = serving,
        multiplier = number_of_units.toDoubleOrNull() ?: 1.0
    )
}

fun FatSecretDto.ServingDto.toDomain(): Serving = Serving(
    id = serving_id,
    description = serving_description,
    calories = calorieDouble(),
    protein = proteinDouble(),
    carbs = carbDouble(),
    fat = fatDouble(),
    fiber = fiberDouble(),
    sodium = sodiumDouble(),
    calcium = calciumDouble(),
    iron = ironDouble(),
    vitaminA = vitaminADouble(),
    vitaminC = vitaminCDouble(),
    vitaminD = vitaminDDouble(),
    potassium = potassiumDouble()
)
