package com.example.fse.domain.model

data class Food(
    val id: Long,
    val name: String,
    val brandName: String?,
    val foodType: String,
    val servings: List<Serving>
)

data class Serving(
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
)

fun Serving.microMap(): Map<Nutrient, Double> = mapOf(
    Nutrient.Fiber to fiber,
    Nutrient.Sodium to sodium,
    Nutrient.Calcium to calcium,
    Nutrient.Iron to iron,
    Nutrient.VitaminA to vitaminA,
    Nutrient.VitaminC to vitaminC,
    Nutrient.VitaminD to vitaminD,
    Nutrient.Potassium to potassium
)
