package com.example.fse.data.repository

import com.example.fse.data.api.FatSecretClient
import com.example.fse.data.api.FatSecretProfileDto
import com.example.fse.domain.model.SavedMeal
import com.example.fse.domain.model.SavedMealItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MealsRepository(
    private val oauth1TokenStore: com.example.fse.data.auth.OAuth1TokenStore,
    private val mealsCache: com.example.fse.data.local.SearchCache<List<SavedMeal>> = com.example.fse.data.local.SearchCache(ttlMs = 2 * 60 * 1000)
) {
    private val json = FatSecretClient.json

    private suspend fun getProfileApi(): com.example.fse.data.api.FatSecretProfileApi? =
        oauth1TokenStore.getTokens()?.let { (token, secret) ->
            FatSecretClient.createProfileApi(token, secret)
        }

    /**
     * Flow of user's saved meals from FatSecret (when connected).
     * Each meal includes its items (foods).
     */
    fun getSavedMeals(): Flow<List<SavedMeal>> = oauth1TokenStore.hasTokens
        .flatMapLatest { hasTokens ->
            flow {
                if (hasTokens) {
                    val meals = withContext(Dispatchers.IO) { fetchSavedMealsWithItems() }
                    emit(meals)
                } else {
                    emit(emptyList())
                }
            }
        }

    private suspend fun fetchSavedMealsWithItems(): List<SavedMeal> {
        mealsCache.get("saved_meals")?.let { return it }
        val api = getProfileApi() ?: return emptyList()
        val response = runCatching { api.getSavedMeals() }.getOrNull() ?: return emptyList()
        if (!response.isSuccessful) return emptyList()
        val mealList = response.body()?.saved_meals?.mealList(json) ?: return emptyList()
        val meals = mealList.mapNotNull { dto ->
            val mealId = dto.saved_meal_id.toLongOrNull() ?: return@mapNotNull null
            val items = fetchMealItems(api, mealId)
            SavedMeal(
                id = mealId,
                name = dto.saved_meal_name,
                description = dto.saved_meal_description?.takeIf { it.isNotBlank() },
                mealTypes = dto.meals?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                items = items
            )
        }
        mealsCache.put("saved_meals", meals)
        return meals
    }

    private suspend fun fetchMealItems(
        api: com.example.fse.data.api.FatSecretProfileApi,
        mealId: Long
    ): List<SavedMealItem> {
        val response = runCatching { api.getSavedMealItems(mealId) }.getOrNull() ?: return emptyList()
        if (!response.isSuccessful) return emptyList()
        val itemList = response.body()?.saved_meal_items?.itemList(json) ?: return emptyList()
        return itemList.mapNotNull { dto ->
            SavedMealItem(
                id = dto.saved_meal_item_id.toLongOrNull() ?: 0L,
                foodId = dto.food_id.toLongOrNull() ?: 0L,
                name = dto.saved_meal_item_name,
                servingId = dto.serving_id.toLongOrNull() ?: 0L,
                numberOfUnits = dto.number_of_units.toDoubleOrNull() ?: 1.0
            )
        }
    }
}
