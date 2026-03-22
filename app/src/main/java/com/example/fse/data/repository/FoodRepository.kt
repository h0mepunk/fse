package com.example.fse.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi

import com.example.fse.data.api.FatSecretClient
import com.example.fse.data.api.FatSecretDto
import com.example.fse.data.api.FatSecretProfileApi
import com.example.fse.data.api.toDiaryEntry
import com.example.fse.data.api.toDomain
import com.example.fse.data.local.LocalDiaryStore
import com.example.fse.data.local.LocalFavoritesStore
import com.example.fse.data.local.LocalRecentFoodStore
import com.example.fse.data.local.StoredFood
import com.example.fse.domain.model.Food
import com.example.fse.domain.model.SavedMeal
import com.example.fse.domain.model.Serving
import com.example.fse.domain.model.DiaryEntry
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.Nutrient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class FoodRepository(
    private val fatSecretAuth: com.example.fse.data.auth.FatSecretAuth,
    private val recentStore: LocalRecentFoodStore,
    private val diaryStore: LocalDiaryStore,
    private val favoritesStore: LocalFavoritesStore,
    private val oauth1TokenStore: com.example.fse.data.auth.OAuth1TokenStore,
    private val searchCache: com.example.fse.data.local.SearchCache<List<Food>> = com.example.fse.data.local.SearchCache()
) {
    data class AddToDiaryRequest(
        val date: LocalDate,
        val mealType: MealType,
        val food: Food,
        val serving: Serving,
        val multiplier: Double = 1.0
    )

    private suspend fun getApi(): com.example.fse.data.api.FatSecretApi? {
        oauth1TokenStore.getTokens()?.let { (token, secret) ->
            return FatSecretClient.createPlatformApiWithOAuth1(token, secret)
        }
        return fatSecretAuth.getAccessToken().getOrNull()?.let { fatSecretAuth.createApi(it) }
    }

    private suspend fun getProfileApi(): FatSecretProfileApi? =
        oauth1TokenStore.getTokens()?.let { (token, secret) ->
            FatSecretClient.createProfileApi(token, secret)
        }

    private val json = FatSecretClient.json
    private val diaryRefreshTrigger = MutableStateFlow(0)

    suspend fun searchFoods(query: String, page: Int = 0): Result<List<Food>> {
        val cacheKey = "${query.lowercase()}_$page"
        searchCache.get(cacheKey)?.let { return Result.success(it) }
        val api = getApi() ?: return Result.failure(Exception("Not authenticated"))
        val useOAuth1 = oauth1TokenStore.getTokens() != null
        return runCatching {
            val foods = if (useOAuth1) {
                val response = api.searchFoodsV1(query, page)
                if (!response.isSuccessful) throw Exception("Search failed: ${response.code()}")
                val wrapper = response.body() ?: throw Exception("Empty response")
                wrapper.error?.let { throw Exception(it.message ?: "API error ${it.code}") }
                val search = wrapper.foods ?: return@runCatching emptyList()
                search.foodList(json).map { it.toDomain() }
            } else {
                val response = api.searchFoodsV5(query, page)
                if (!response.isSuccessful) throw Exception("Search failed: ${response.code()}")
                val wrapper = response.body() ?: throw Exception("Empty response")
                wrapper.error?.let { throw Exception(it.message ?: "API error ${it.code}") }
                val search = wrapper.foods_search ?: return@runCatching emptyList()
                search.foodList(json).map { it.toDomain() }
            }
            val enriched = coroutineScope {
                foods.map { food ->
                    async {
                        val hasNutrition = food.servings.any { s ->
                            s.calories > 0 || s.protein > 0 || s.carbs > 0 || s.fat > 0
                        }
                        if (hasNutrition) food else getFood(food.id).getOrNull() ?: food
                    }
                }.awaitAll()
            }
            searchCache.put(cacheKey, enriched)
            enriched
        }
    }

    suspend fun getFood(foodId: Long): Result<Food> {
        val api = getApi() ?: return Result.failure(Exception("Not authenticated"))
        return runCatching {
            val response = api.getFood(foodId)
            if (!response.isSuccessful) throw Exception("Get food failed: ${response.code()}")
            val wrapper = response.body() ?: throw Exception("Empty response")
            wrapper.error?.let { throw Exception(it.message ?: "API error ${it.code}") }
            val foodDto = wrapper.food ?: throw Exception("Food not found")
            foodDto.toDomain(json)
        }
    }

    fun getRecentFoods(): Flow<List<Food>> = combine(
        oauth1TokenStore.hasTokens,
        recentStore.foods
    ) { hasOAuth1, localFoods -> Pair(hasOAuth1, localFoods) }
        .flatMapLatest { (hasOAuth1, localFoods) ->
            flow {
                if (hasOAuth1) {
                    val profile = withContext(Dispatchers.IO) {
                        getProfileApi()?.let { api ->
                            runCatching { api.getRecentlyEaten() }.getOrNull()?.body()
                                ?.foods?.foodList(json)?.map { it.toDomain() } ?: emptyList()
                        } ?: emptyList()
                    }
                    emit(if (profile.isEmpty()) localFoods.map { it.toDomain() } else profile)
                } else {
                    emit(localFoods.map { it.toDomain() })
                }
            }
        }

    suspend fun addToRecent(food: Food) {
        recentStore.add(StoredFood.from(food))
    }

    fun getRecentFoodsForMeal(
        mealType: MealType,
        date: LocalDate,
        daysBack: Long = 30
    ): Flow<List<Food>> = combine(
        diaryStore.allEntries(),
        getRecentFoods()
    ) { allEntries, fallback ->
        val fromDate = date.minusDays(daysBack - 1)
        val foodsByMeal = allEntries
            .asSequence()
            .filter { entry ->
                !entry.date.isBefore(fromDate) &&
                    !entry.date.isAfter(date) &&
                    entry.mealType == mealType
            }
            .sortedByDescending { it.date }
            .map { it.food }
            .distinctBy { it.id }
            .take(20)
            .toList()
        if (foodsByMeal.isNotEmpty()) foodsByMeal else fallback
    }

    fun getRecentFoodsForMealType(mealType: MealType, date: LocalDate): Flow<List<Food>> =
        diaryStore.allEntries().map { allEntries ->
            val fromDate = date.minusDays(30)
            allEntries
                .asSequence()
                .filter { it.mealType == mealType && !it.date.isBefore(fromDate) && !it.date.isAfter(date) }
                .sortedByDescending { it.date }
                .map { it.food }
                .distinctBy { it.id }
                .take(40)
                .toList()
        }

    fun getRecentFoodsForMeal(
        mealType: MealType,
        referenceDate: LocalDate = LocalDate.now(),
        lookbackDays: Long = 30
    ): Flow<List<Food>> = diaryStore.allEntries().map { entries ->
        val fromDate = referenceDate.minusDays(lookbackDays)
        entries
            .asSequence()
            .filter { entry ->
                entry.mealType == mealType &&
                    !entry.date.isBefore(fromDate) &&
                    !entry.date.isAfter(referenceDate)
            }
            .sortedByDescending { it.date }
            .distinctBy { it.food.id }
            .map { it.food }
            .toList()
    }

    fun getDiary(date: LocalDate): Flow<List<DiaryEntry>> = combine(
        oauth1TokenStore.hasTokens,
        diaryStore.entriesForDate(date),
        diaryRefreshTrigger
    ) { hasOAuth1, localEntries, _ -> Pair(hasOAuth1, localEntries) }
        .flatMapLatest { (hasOAuth1, localEntries) ->
            flow {
                if (hasOAuth1) {
                    val dateInt = ChronoUnit.DAYS.between(LocalDate.of(1970, 1, 1), date).toInt()
                    val profile = withContext(Dispatchers.IO) {
                        getProfileApi()?.let { api ->
                            runCatching { api.getFoodEntries(dateInt) }.getOrNull()?.body()
                                ?.food_entries?.entryList(json)?.map { it.toDiaryEntry() } ?: emptyList()
                        } ?: emptyList()
                    }
                    val localIds = localEntries.map { it.id }.toSet()
                    val merged = localEntries + profile.filter { it.id !in localIds }
                    emit(merged.sortedWith(compareBy({ it.mealType.ordinal }, { it.id })))
                } else {
                    emit(localEntries)
                }
            }
        }

    fun getDiaryNutrientTotals(date: LocalDate): Flow<Map<Nutrient, Double>> =
        getDiary(date).map { entries ->
            val totals = mutableMapOf<Nutrient, Double>()
            Nutrient.entries.forEach { totals[it] = 0.0 }
            entries.forEach { entry ->
                val mult = entry.multiplier
                totals[Nutrient.Calories] = (totals[Nutrient.Calories] ?: 0.0) + entry.serving.calories * mult
                totals[Nutrient.Protein] = (totals[Nutrient.Protein] ?: 0.0) + entry.serving.protein * mult
                totals[Nutrient.Carbs] = (totals[Nutrient.Carbs] ?: 0.0) + entry.serving.carbs * mult
                totals[Nutrient.Fat] = (totals[Nutrient.Fat] ?: 0.0) + entry.serving.fat * mult
                totals[Nutrient.Fiber] = (totals[Nutrient.Fiber] ?: 0.0) + entry.serving.fiber * mult
                totals[Nutrient.Sodium] = (totals[Nutrient.Sodium] ?: 0.0) + entry.serving.sodium * mult
                totals[Nutrient.Calcium] = (totals[Nutrient.Calcium] ?: 0.0) + entry.serving.calcium * mult
                totals[Nutrient.Iron] = (totals[Nutrient.Iron] ?: 0.0) + entry.serving.iron * mult
                totals[Nutrient.VitaminA] = (totals[Nutrient.VitaminA] ?: 0.0) + entry.serving.vitaminA * mult
                totals[Nutrient.VitaminC] = (totals[Nutrient.VitaminC] ?: 0.0) + entry.serving.vitaminC * mult
                totals[Nutrient.VitaminD] = (totals[Nutrient.VitaminD] ?: 0.0) + entry.serving.vitaminD * mult
                totals[Nutrient.Potassium] = (totals[Nutrient.Potassium] ?: 0.0) + entry.serving.potassium * mult
            }
            totals.toMap()
        }

    fun getWeeklyNutrientTotals(weekEndDate: LocalDate): Flow<Map<Nutrient, Double>> =
        diaryStore.allEntries().map { all ->
            val start = weekEndDate.minusDays(6)
            val entries = all.filter { e -> !e.date.isBefore(start) && !e.date.isAfter(weekEndDate) }
            val totals = mutableMapOf<Nutrient, Double>()
            Nutrient.entries.forEach { totals[it] = 0.0 }
            entries.forEach { entry ->
                val mult = entry.multiplier
                totals[Nutrient.Calories] = (totals[Nutrient.Calories] ?: 0.0) + entry.serving.calories * mult
                totals[Nutrient.Protein] = (totals[Nutrient.Protein] ?: 0.0) + entry.serving.protein * mult
                totals[Nutrient.Carbs] = (totals[Nutrient.Carbs] ?: 0.0) + entry.serving.carbs * mult
                totals[Nutrient.Fat] = (totals[Nutrient.Fat] ?: 0.0) + entry.serving.fat * mult
                totals[Nutrient.Fiber] = (totals[Nutrient.Fiber] ?: 0.0) + entry.serving.fiber * mult
                totals[Nutrient.Sodium] = (totals[Nutrient.Sodium] ?: 0.0) + entry.serving.sodium * mult
                totals[Nutrient.Calcium] = (totals[Nutrient.Calcium] ?: 0.0) + entry.serving.calcium * mult
                totals[Nutrient.Iron] = (totals[Nutrient.Iron] ?: 0.0) + entry.serving.iron * mult
                totals[Nutrient.VitaminA] = (totals[Nutrient.VitaminA] ?: 0.0) + entry.serving.vitaminA * mult
                totals[Nutrient.VitaminC] = (totals[Nutrient.VitaminC] ?: 0.0) + entry.serving.vitaminC * mult
                totals[Nutrient.VitaminD] = (totals[Nutrient.VitaminD] ?: 0.0) + entry.serving.vitaminD * mult
                totals[Nutrient.Potassium] = (totals[Nutrient.Potassium] ?: 0.0) + entry.serving.potassium * mult
            }
            totals.toMap()
        }

    private suspend fun createRemoteFoodEntry(
        profileApi: FatSecretProfileApi,
        request: AddToDiaryRequest
    ): DiaryEntry {
        val dateInt = ChronoUnit.DAYS.between(LocalDate.of(1970, 1, 1), request.date).toInt()
        val mealValue = when (request.mealType) {
            MealType.Breakfast -> "breakfast"
            MealType.Lunch -> "lunch"
            MealType.Dinner -> "dinner"
            MealType.Snack -> "snack"
            MealType.Other -> "other"
        }
        val response = profileApi.createFoodEntry(
            foodId = request.food.id,
            foodEntryName = request.food.name,
            servingId = request.serving.id,
            numberOfUnits = request.multiplier,
            meal = mealValue,
            dateInt = dateInt
        )
        if (!response.isSuccessful) {
            throw Exception("Create food entry failed: ${response.code()}")
        }
        val created = response.body()
            ?.food_entries
            ?.entryList(json)
            ?.firstOrNull()
            ?.toDiaryEntry()
            ?: throw Exception("Create food entry failed: empty response")
        return created.copy(mealType = request.mealType)
    }

    suspend fun addToDiary(request: AddToDiaryRequest): DiaryEntry {
        val createdEntry = if (oauth1TokenStore.getTokens() != null) {
            val profileApi = getProfileApi() ?: throw Exception("Not authenticated")
            createRemoteFoodEntry(profileApi, request)
        } else {
            DiaryEntry(
                id = "d_${System.currentTimeMillis()}_${request.food.id}",
                date = request.date,
                mealType = request.mealType,
                food = request.food,
                serving = request.serving,
                multiplier = request.multiplier
            )
        }
        diaryStore.add(createdEntry)
        addToRecent(createdEntry.food)
        diaryRefreshTrigger.value++
        return createdEntry
    }

    suspend fun removeFromDiary(entryId: String) {
        if (oauth1TokenStore.getTokens() != null && entryId.all { it.isDigit() }) {
            val profileApi = getProfileApi() ?: throw Exception("Not authenticated")
            val response = profileApi.deleteFoodEntry(foodEntryId = entryId)
            if (!response.isSuccessful) {
                throw Exception("Delete food entry failed: ${response.code()}")
            }
            val ok = response.body()?.success?.value == "1"
            if (!ok) throw Exception("Delete food entry failed")
        }
        diaryStore.remove(entryId)
        diaryRefreshTrigger.value++
    }

    fun forceDiaryRefresh() {
        diaryRefreshTrigger.value++
    }

    /**
     * Adds all items of a saved meal to today's diary.
     * Fetches full food details for each item to get nutrition.
     */
    suspend fun addSavedMealToDiary(meal: SavedMeal): Result<Unit> {
        val date = LocalDate.now()
        return runCatching {
            meal.items.forEach { item ->
                val foodResult = getFood(item.foodId)
                val food = foodResult.getOrThrow()
                val serving = food.servings.find { it.id == item.servingId }
                    ?: food.servings.firstOrNull()
                    ?: throw Exception("No serving for ${item.name}")
                addToDiary(
                    AddToDiaryRequest(
                        date = date,
                        mealType = MealType.Other,
                        food = food,
                        serving = serving,
                        multiplier = item.numberOfUnits
                    )
                )
            }
        }
    }

    fun getFavorites(): Flow<List<Food>> = combine(
        oauth1TokenStore.hasTokens,
        favoritesStore.foods
    ) { hasOAuth1, localFoods -> Pair(hasOAuth1, localFoods) }
        .flatMapLatest { (hasOAuth1, localFoods) ->
            flow {
                if (hasOAuth1) {
                    val profile = withContext(Dispatchers.IO) {
                        getProfileApi()?.let { api ->
                            runCatching { api.getFavorites() }.getOrNull()?.body()
                                ?.foods?.foodList(json)?.map { it.toDomain() } ?: emptyList()
                        } ?: emptyList()
                    }
                    emit(if (profile.isEmpty()) localFoods.map { it.toDomain() } else profile)
                } else {
                    emit(localFoods.map { it.toDomain() })
                }
            }
        }

    suspend fun addFavorite(food: Food) {
        favoritesStore.add(StoredFood.from(food))
    }

    suspend fun removeFavorite(foodId: Long) {
        favoritesStore.remove(foodId)
    }

    suspend fun isFavorite(foodId: Long): Boolean = favoritesStore.contains(foodId)

    fun getFavoritesRichIn(nutrient: Nutrient, minAmount: Double): Flow<List<Food>> =
        favoritesStore.foods.map { list ->
            list.map { it.toDomain() }.filter { food ->
                food.servings.any { serving ->
                    val value = when (nutrient) {
                        Nutrient.Calories -> serving.calories
                        Nutrient.Protein -> serving.protein
                        Nutrient.Carbs -> serving.carbs
                        Nutrient.Fat -> serving.fat
                        Nutrient.Fiber -> serving.fiber
                        Nutrient.Sodium -> serving.sodium
                        Nutrient.Calcium -> serving.calcium
                        Nutrient.Iron -> serving.iron
                        Nutrient.VitaminA -> serving.vitaminA
                        Nutrient.VitaminC -> serving.vitaminC
                        Nutrient.VitaminD -> serving.vitaminD
                        Nutrient.Potassium -> serving.potassium
                    }
                    value >= minAmount
                }
            }
        }
}
