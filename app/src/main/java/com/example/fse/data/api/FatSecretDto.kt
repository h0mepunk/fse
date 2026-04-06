package com.example.fse.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Extracts Double from JsonElement - accepts both number and string. */
private fun JsonElement?.toDouble(): Double = when (this) {
    null -> 0.0
    is JsonPrimitive -> content.toDoubleOrNull() ?: 0.0
    else -> 0.0
}

/**
 * FatSecret API DTOs.
 * Note: API often returns numbers as strings (e.g. "177") or raw numbers; food/recipe can be single or array.
 */
object FatSecretDto {

    @Serializable
    data class FoodsSearchWrapper(
        val foods_search: FoodsSearchResponse? = null,
        val error: ErrorResponse? = null
    )

    /** v1 search response - root is "foods" */
    @Serializable
    data class FoodsSearchWrapperV1(
        val foods: FoodsResponseV1? = null,
        val error: ErrorResponse? = null
    )

    @Serializable
    data class FoodsResponseV1(
        val food: JsonElement? = null,
        val max_results: Int = 20,
        val total_results: Int = 0,
        val page_number: Int = 0
    ) {
        fun foodList(json: kotlinx.serialization.json.Json): List<FoodSearchItemV1> {
            val f = food ?: return emptyList()
            return try {
                when {
                    f is JsonArray -> f.map { json.decodeFromJsonElement(FoodSearchItemV1.serializer(), it) }
                    else -> listOf(json.decodeFromJsonElement(FoodSearchItemV1.serializer(), f))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    @Serializable
    data class FoodSearchItemV1(
        val food_id: Long = 0,
        val food_name: String = "",
        val food_type: String = "Generic",
        val food_url: String? = null,
        val brand_name: String? = null,
        val food_description: String? = null
    )

    @Serializable
    data class FoodsSearchResponse(
        val max_results: Int = 20,
        val total_results: Int = 0,
        val page_number: Int = 0,
        val food: JsonElement? = null,
        val results: ResultsWrapper? = null
    ) {
        fun foodList(json: kotlinx.serialization.json.Json): List<FoodSearchItem> {
            val f = results?.food ?: food ?: return emptyList()
            return try {
                when {
                    f is JsonArray -> f.map { json.decodeFromJsonElement(FoodSearchItem.serializer(), it) }
                    else -> listOf(json.decodeFromJsonElement(FoodSearchItem.serializer(), f))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    @Serializable
    data class ResultsWrapper(val food: JsonElement? = null)

    @Serializable
    data class FoodSearchItem(
        val food_id: Long = 0,
        val food_name: String = "",
        val food_type: String = "Generic",
        val food_url: String? = null,
        val brand_name: String? = null,
        val food_description: String? = null,
        val servings: ServingsWrapper? = null
    ) {
        fun servingList(json: kotlinx.serialization.json.Json): List<ServingDto> {
            val s = servings?.serving ?: return emptyList()
            val elements: List<JsonElement> = when (s) {
                is JsonArray -> s
                else -> listOf(s)
            }
            return elements.mapNotNull { parseServingFromJson(it) }
        }
    }

    @Serializable
    data class ServingsWrapper(val serving: JsonElement? = null)

    /** Parses serving from JsonObject - handles both string and number for nutrition fields. */
    fun parseServingFromJson(element: JsonElement): ServingDto? = try {
        val obj = element as? JsonObject ?: return null
        fun id(el: JsonElement?) = when (el) {
            null -> 0L
            is JsonPrimitive -> el.content.toLongOrNull() ?: 0L
            else -> 0L
        }
        fun str(el: JsonElement?) = (el as? JsonPrimitive)?.content ?: ""
        fun num(el: JsonElement?) = el.toDouble()
        ServingDto(
            serving_id = id(obj["serving_id"]),
            serving_description = str(obj["serving_description"]),
            calories = obj["calories"]?.let { num(it).toString() },
            carbohydrate = obj["carbohydrate"]?.let { num(it).toString() },
            protein = obj["protein"]?.let { num(it).toString() },
            fat = obj["fat"]?.let { num(it).toString() },
            saturated_fat = obj["saturated_fat"]?.let { num(it).toString() },
            sodium = obj["sodium"]?.let { num(it).toString() },
            potassium = obj["potassium"]?.let { num(it).toString() },
            fiber = obj["fiber"]?.let { num(it).toString() },
            sugar = obj["sugar"]?.let { num(it).toString() },
            vitamin_a = obj["vitamin_a"]?.let { num(it).toString() },
            vitamin_c = obj["vitamin_c"]?.let { num(it).toString() },
            vitamin_d = obj["vitamin_d"]?.let { num(it).toString() },
            calcium = obj["calcium"]?.let { num(it).toString() },
            iron = obj["iron"]?.let { num(it).toString() }
        )
    } catch (_: Exception) {
        null
    }

    @Serializable
    data class ServingDto(
        val serving_id: Long = 0,
        val serving_description: String = "",
        val calories: String? = null,
        val carbohydrate: String? = null,
        val protein: String? = null,
        val fat: String? = null,
        val saturated_fat: String? = null,
        val sodium: String? = null,
        val potassium: String? = null,
        val fiber: String? = null,
        val sugar: String? = null,
        val vitamin_a: String? = null,
        val vitamin_c: String? = null,
        val vitamin_d: String? = null,
        val calcium: String? = null,
        val iron: String? = null
    ) {
        fun calorieDouble(): Double = calories.toDoubleOrNull() ?: 0.0
        fun proteinDouble(): Double = protein.toDoubleOrNull() ?: 0.0
        fun carbDouble(): Double = carbohydrate.toDoubleOrNull() ?: 0.0
        fun fatDouble(): Double = fat.toDoubleOrNull() ?: 0.0
        fun fiberDouble(): Double = fiber.toDoubleOrNull() ?: 0.0
        fun sodiumDouble(): Double = sodium.toDoubleOrNull() ?: 0.0
        fun potassiumDouble(): Double = potassium.toDoubleOrNull() ?: 0.0
        fun calciumDouble(): Double = calcium.toDoubleOrNull() ?: 0.0
        fun ironDouble(): Double = iron.toDoubleOrNull() ?: 0.0
        fun vitaminADouble(): Double = vitamin_a.toDoubleOrNull() ?: 0.0
        fun vitaminCDouble(): Double = vitamin_c.toDoubleOrNull() ?: 0.0
        fun vitaminDDouble(): Double = vitamin_d.toDoubleOrNull() ?: 0.0
    }

    private fun String?.toDoubleOrNull(): Double? = this?.toDoubleOrNull()

    @Serializable
    data class FoodGetWrapper(
        val food: FoodDetailDto? = null,
        val error: ErrorResponse? = null
    )

    @Serializable
    data class FoodDetailDto(
        val food_id: Long = 0,
        val food_name: String = "",
        val food_type: String = "Generic",
        val food_url: String? = null,
        val brand_name: String? = null,
        val servings: ServingsWrapper? = null
    ) {
        fun servingList(json: kotlinx.serialization.json.Json): List<ServingDto> {
            val s = servings?.serving ?: return emptyList()
            val elements: List<JsonElement> = when (s) {
                is JsonArray -> s
                else -> listOf(s)
            }
            return elements.mapNotNull { parseServingFromJson(it) }
        }
    }

    @Serializable
    data class RecipeGetV2Wrapper(
        val recipe: JsonElement? = null,
        val error: ErrorResponse? = null
    )

    @Serializable
    data class RecipesSearchWrapper(
        val recipes: RecipesResponse? = null,
        val error: ErrorResponse? = null
    )

    @Serializable
    data class RecipesResponse(
        val max_results: Int = 20,
        val total_results: Int = 0,
        val page_number: Int = 0,
        val recipe: JsonElement? = null
    ) {
        fun recipeList(json: kotlinx.serialization.json.Json): List<RecipeItemDto> {
            val r = recipe ?: return emptyList()
            return try {
                when {
                    r is JsonArray -> r.map { json.decodeFromJsonElement(RecipeItemDto.serializer(), it) }
                    else -> listOf(json.decodeFromJsonElement(RecipeItemDto.serializer(), r))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    @Serializable
    data class RecipeItemDto(
        val recipe_id: Long = 0,
        val recipe_name: String = "",
        val recipe_description: String? = null,
        val recipe_image: String? = null,
        val recipe_nutrition: RecipeNutritionDto? = null,
        val recipe_ingredients: RecipeIngredientsWrapper? = null,
        val recipe_types: RecipeTypesWrapper? = null
    )

    @Serializable
    data class RecipeNutritionDto(
        val calories: String? = null,
        val carbohydrate: String? = null,
        val protein: String? = null,
        val fat: String? = null
    )

    @Serializable
    data class RecipeIngredientsWrapper(val ingredient: JsonElement? = null)

    @Serializable
    data class RecipeTypesWrapper(val recipe_type: JsonElement? = null)

    @Serializable
    data class ErrorResponse(
        val code: Int = 0,
        val message: String? = null
    )

    @Serializable
    data class TokenResponse(
        @SerialName("access_token") val accessToken: String = "",
        @SerialName("token_type") val tokenType: String = "Bearer",
        @SerialName("expires_in") val expiresIn: Long = 0
    )
}
