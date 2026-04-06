package com.example.fse.data.repository

import com.example.fse.data.api.FatSecretClient
import com.example.fse.data.api.FatSecretProfileApi
import com.example.fse.data.local.SearchCache
import com.example.fse.domain.model.Food
import com.example.fse.domain.model.ProfileSavedMeal
import com.example.fse.domain.model.ProfileSavedMealItem
import com.example.fse.domain.model.Serving
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class SavedMealsRepository(
    private val oauth1TokenStore: com.example.fse.data.auth.OAuth1TokenStore
) {
    private val json = FatSecretClient.json
    private val refreshTrigger = MutableStateFlow(0)
    private val cache = SearchCache<List<ProfileSavedMeal>>(ttlMs = 24 * 60 * 60 * 1000L)
    private val cacheKey = "profile_saved_meals"

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** После pull-to-refresh данные уже в кэше — не запускаем второй фоновый запрос в том же цикле. */
    private val skipNextBackgroundFetch = AtomicBoolean(false)

    private suspend fun getProfileApi(): FatSecretProfileApi? =
        oauth1TokenStore.getTokens()?.let { (token, secret) ->
            FatSecretClient.createProfileApi(token, secret)
        }

    fun triggerRefresh() {
        refreshTrigger.value++
    }

    /**
     * Сохранённые приёмы: сразу кэш (как дневник), затем обновление в фоне.
     * [triggerRefresh] и pull-to-refresh перезапускают загрузку.
     */
    fun getSavedMeals(): Flow<List<ProfileSavedMeal>> = combine(
        oauth1TokenStore.hasTokens,
        refreshTrigger
    ) { hasTokens, version -> Pair(hasTokens, version) }
        .flatMapLatest { (hasTokens, _) ->
            channelFlow {
                if (!hasTokens) {
                    send(withContext(Dispatchers.IO) { cache.get(cacheKey) } ?: emptyList())
                    return@channelFlow
                }
                val cached = withContext(Dispatchers.IO) { cache.get(cacheKey) }
                send(cached ?: emptyList())
                val skipBg = skipNextBackgroundFetch.getAndSet(false)
                if (skipBg) return@channelFlow
                launch(Dispatchers.IO) {
                    val api = getProfileApi() ?: return@launch
                    val fresh = loadSavedMealsFromApi(api)
                    if (fresh != null) {
                        cache.put(cacheKey, fresh)
                        send(fresh)
                    }
                }
            }
        }

    /**
     * Один полный запрос для pull-to-refresh; UI обновится по [refreshTrigger] без дублирующего фонового запроса.
     */
    suspend fun refreshSavedMealsAndAwait() {
        if (oauth1TokenStore.getTokens() == null) return
        withContext(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                val api = getProfileApi() ?: return@withContext
                val fresh = loadSavedMealsFromApi(api)
                if (fresh != null) {
                    cache.put(cacheKey, fresh)
                    skipNextBackgroundFetch.set(true)
                    refreshTrigger.value++
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** null — ошибка сети/ответа; кэш не затираем. */
    private suspend fun loadSavedMealsFromApi(api: FatSecretProfileApi): List<ProfileSavedMeal>? {
        val response = runCatching { api.getSavedMeals() }.getOrNull() ?: return null
        if (!response.isSuccessful) return null
        val meals = response.body()?.saved_meals?.mealList(json) ?: return emptyList()
        if (meals.isEmpty()) return emptyList()
        return coroutineScope {
            meals.map { dto ->
                async {
                    val id = dto.saved_meal_id.toLongOrNull() ?: return@async null
                    val itemsResp = runCatching { api.getSavedMealItems(id) }.getOrNull()
                    val itemDtos = if (itemsResp?.isSuccessful == true) {
                        itemsResp.body()?.saved_meal_items?.itemList(json) ?: emptyList()
                    } else {
                        emptyList()
                    }
                    ProfileSavedMeal(
                        id = dto.saved_meal_id,
                        name = dto.saved_meal_name,
                        description = dto.saved_meal_description,
                        suitableMeals = dto.meals,
                        items = itemDtos.map { e ->
                            ProfileSavedMealItem(
                                itemId = e.saved_meal_item_id,
                                foodId = e.food_id.toLongOrNull() ?: 0L,
                                name = e.saved_meal_item_name,
                                servingId = e.serving_id.toLongOrNull() ?: 0L,
                                numberOfUnits = e.number_of_units.toDoubleOrNull() ?: 1.0
                            )
                        }
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    suspend fun editSavedMealItemUnits(itemId: String, numberOfUnits: Double) {
        val api = getProfileApi() ?: throw Exception("Нет доступа к профилю FatSecret")
        val id = itemId.toLongOrNull() ?: throw Exception("Некорректный id позиции")
        val response = api.editSavedMealItem(itemId = id, numberOfUnits = numberOfUnits)
        if (!response.isSuccessful) throw Exception("Не удалось сохранить: ${response.code()}")
        if (response.body()?.success?.value != "1") throw Exception("Не удалось сохранить")
        triggerRefresh()
    }

    suspend fun deleteSavedMealItem(itemId: String) {
        val api = getProfileApi() ?: throw Exception("Нет доступа к профилю FatSecret")
        val id = itemId.toLongOrNull() ?: throw Exception("Некорректный id позиции")
        val response = api.deleteSavedMealItem(itemId = id)
        if (!response.isSuccessful) throw Exception("Не удалось удалить: ${response.code()}")
        if (response.body()?.success?.value != "1") throw Exception("Не удалось удалить")
        triggerRefresh()
    }

    suspend fun addFoodToSavedMeal(savedMealId: String, food: Food, serving: Serving, numberOfUnits: Double) {
        val api = getProfileApi() ?: throw Exception("Нет доступа к профилю FatSecret")
        val mealId = savedMealId.toLongOrNull() ?: throw Exception("Некорректный id приёма")
        val response = api.addSavedMealItem(
            savedMealId = mealId,
            foodId = food.id,
            itemName = food.name,
            servingId = serving.id,
            numberOfUnits = numberOfUnits
        )
        if (!response.isSuccessful) throw Exception("Не удалось добавить: ${response.code()}")
        val newId = response.body()?.idLong()
        if (newId == null || newId <= 0L) throw Exception("Не удалось добавить")
        triggerRefresh()
    }
}
