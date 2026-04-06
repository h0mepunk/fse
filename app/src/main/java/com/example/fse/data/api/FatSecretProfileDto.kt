package com.example.fse.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * DTOs for FatSecret Profile API (3-legged OAuth).
 * @see <a href="https://platform.fatsecret.com/docs/v2/food_entries.get">food_entries.get</a>
 * @see <a href="https://platform.fatsecret.com/docs/v2/foods.get_favorites">foods.get_favorites</a>
 * @see <a href="https://platform.fatsecret.com/docs/v2/foods.get_recently_eaten">foods.get_recently_eaten</a>
 * @see <a href="https://platform.fatsecret.com/docs/v2/recipes.get_favorites">recipes.get_favorites</a>
 * @see <a href="https://platform.fatsecret.com/docs/v2/saved_meals.get">saved_meals.get</a>
 */
object FatSecretProfileDto {

    @Serializable
    data class FoodEntriesWrapper(
        val food_entries: FoodEntriesResponse? = null
    )

    @Serializable
    data class FoodEntriesResponse(
        val food_entry: JsonElement? = null
    ) {
        fun entryList(json: kotlinx.serialization.json.Json): List<FoodEntryItem> {
            val e = food_entry ?: return emptyList()
            return try {
                when {
                    e is JsonArray -> e.map { json.decodeFromJsonElement(FoodEntryItem.serializer(), it) }
                    else -> listOf(json.decodeFromJsonElement(FoodEntryItem.serializer(), e))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    @Serializable
    data class FoodEntryItem(
        val food_entry_id: String = "",
        val food_id: String = "",
        val serving_id: String = "",
        val date_int: String = "",
        val meal: String = "",
        val number_of_units: String = "1",
        val food_entry_name: String = "",
        val food_entry_description: String = "",
        val calories: String? = null,
        val carbohydrate: String? = null,
        val protein: String? = null,
        val fat: String? = null
    )

    @Serializable
    data class ProfileFoodsWrapper(
        val foods: ProfileFoodsResponse? = null
    )

    @Serializable
    data class ProfileFoodsResponse(
        val food: JsonElement? = null
    ) {
        fun foodList(json: kotlinx.serialization.json.Json): List<ProfileFoodItem> {
            val f = food ?: return emptyList()
            return try {
                when {
                    f is JsonArray -> f.map { json.decodeFromJsonElement(ProfileFoodItem.serializer(), it) }
                    else -> listOf(json.decodeFromJsonElement(ProfileFoodItem.serializer(), f))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    @Serializable
    data class ProfileFoodItem(
        val food_id: String = "",
        val food_name: String = "",
        val food_type: String = "Generic",
        val food_url: String? = null,
        val food_description: String? = null,
        val serving_id: String = "",
        val number_of_units: String = "1"
    )

    @Serializable
    data class ProfileRecipesWrapper(
        val recipes: ProfileRecipesResponse? = null
    )

    @Serializable
    data class ProfileRecipesResponse(
        val recipe: JsonElement? = null
    ) {
        fun recipeList(json: kotlinx.serialization.json.Json): List<ProfileRecipeItem> {
            val r = recipe ?: return emptyList()
            return try {
                when {
                    r is JsonArray -> r.map { json.decodeFromJsonElement(ProfileRecipeItem.serializer(), it) }
                    else -> listOf(json.decodeFromJsonElement(ProfileRecipeItem.serializer(), r))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    @Serializable
    data class SuccessResponse(
        val success: SuccessValue? = null
    )

    @Serializable
    data class SuccessValue(
        val value: String? = null
    )

    @Serializable
    data class ProfileRecipeItem(
        val recipe_id: String = "",
        val recipe_name: String = "",
        val recipe_url: String? = null,
        val recipe_description: String? = null,
        val recipe_image: String? = null
    )

    @Serializable
    data class SavedMealsWrapper(
        val saved_meals: SavedMealsResponse? = null
    )

    @Serializable
    data class SavedMealsResponse(
        val saved_meal: JsonElement? = null
    ) {
        fun mealList(json: kotlinx.serialization.json.Json): List<SavedMealItem> {
            val m = saved_meal ?: return emptyList()
            return try {
                when {
                    m is JsonArray -> m.map { json.decodeFromJsonElement(SavedMealItem.serializer(), it) }
                    else -> listOf(json.decodeFromJsonElement(SavedMealItem.serializer(), m))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    @Serializable
    data class SavedMealItem(
        val saved_meal_id: String = "",
        val saved_meal_name: String = "",
        val saved_meal_description: String? = null,
        val meals: String? = null
    )

    @Serializable
    data class SavedMealItemsWrapper(
        val saved_meal_items: SavedMealItemsResponse? = null
    )

    @Serializable
    data class SavedMealItemsResponse(
        val saved_meal_item: JsonElement? = null
    ) {
        fun itemList(json: kotlinx.serialization.json.Json): List<SavedMealItemEntry> {
            val i = saved_meal_item ?: return emptyList()
            return try {
                when {
                    i is JsonArray -> i.map { json.decodeFromJsonElement(SavedMealItemEntry.serializer(), it) }
                    else -> listOf(json.decodeFromJsonElement(SavedMealItemEntry.serializer(), i))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    @Serializable
    data class SavedMealItemEntry(
        val saved_meal_item_id: String = "",
        val food_id: String = "",
        val saved_meal_item_name: String = "",
        val serving_id: String = "",
        val number_of_units: String = "1"
    )

    @Serializable
    data class SavedMealItemIdResponse(
        val saved_meal_item_id: SuccessValue? = null
    ) {
        fun idLong(): Long? = saved_meal_item_id?.value?.toLongOrNull()
    }
}
