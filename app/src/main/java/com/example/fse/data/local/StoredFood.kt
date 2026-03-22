package com.example.fse.data.local

import com.example.fse.domain.model.Food
import com.example.fse.domain.model.Serving
import kotlinx.serialization.Serializable

@Serializable
data class StoredFood(
    val id: Long,
    val name: String,
    val brandName: String?,
    val foodType: String,
    val servings: List<StoredServing>
) {
    fun toDomain(): Food = Food(
        id = id,
        name = name,
        brandName = brandName,
        foodType = foodType,
        servings = servings.map { it.toDomain() }
    )

    companion object {
        fun from(food: Food): StoredFood = StoredFood(
            id = food.id,
            name = food.name,
            brandName = food.brandName,
            foodType = food.foodType,
            servings = food.servings.map { StoredServing.from(it) }
        )
    }
}

@Serializable
data class StoredServing(
    val id: Long,
    val description: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sodium: Double,
    val calcium: Double,
    val iron: Double,
    val vitaminA: Double,
    val vitaminC: Double,
    val vitaminD: Double,
    val potassium: Double
) {
    fun toDomain(): Serving = Serving(
        id = id,
        description = description,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        fiber = fiber,
        sodium = sodium,
        calcium = calcium,
        iron = iron,
        vitaminA = vitaminA,
        vitaminC = vitaminC,
        vitaminD = vitaminD,
        potassium = potassium
    )

    companion object {
        fun from(s: Serving): StoredServing = StoredServing(
            id = s.id,
            description = s.description,
            calories = s.calories,
            protein = s.protein,
            carbs = s.carbs,
            fat = s.fat,
            fiber = s.fiber,
            sodium = s.sodium,
            calcium = s.calcium,
            iron = s.iron,
            vitaminA = s.vitaminA,
            vitaminC = s.vitaminC,
            vitaminD = s.vitaminD,
            potassium = s.potassium
        )
    }
}
