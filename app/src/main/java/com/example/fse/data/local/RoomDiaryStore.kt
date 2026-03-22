package com.example.fse.data.local

import com.example.fse.data.local.db.dao.DiaryDao
import com.example.fse.data.local.db.dao.FoodDao
import com.example.fse.data.local.db.dao.ServingDao
import com.example.fse.data.local.db.entity.DiaryEntryEntity
import com.example.fse.data.local.db.entity.FoodEntity
import com.example.fse.data.local.db.entity.ServingEntity
import com.example.fse.domain.model.DiaryEntry
import com.example.fse.domain.model.Food
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.Serving
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomDiaryStore(
    private val diaryDao: DiaryDao,
    private val foodDao: FoodDao,
    private val servingDao: ServingDao,
) : LocalDiaryStore {
    override fun allEntries(): Flow<List<DiaryEntry>> = diaryDao.allEntries().map { entities ->
        entities.toDomainList(foodDao, servingDao)
    }

    override fun entriesForDate(date: LocalDate): Flow<List<DiaryEntry>> =
        diaryDao.entriesForDate(date).map { entities ->
            entities.toDomainList(foodDao, servingDao)
        }

    override suspend fun add(entry: DiaryEntry) {
        ensureFoodAndServingExist(entry)
        diaryDao.insert(
            DiaryEntryEntity(
                id = entry.id,
                date = entry.date,
                mealType = entry.mealType.name,
                foodId = entry.food.id,
                servingId = entry.serving.id,
                multiplier = entry.multiplier
            )
        )
    }

    override suspend fun remove(entryId: String) {
        diaryDao.deleteById(entryId)
    }

    private suspend fun ensureFoodAndServingExist(entry: DiaryEntry) {
        foodDao.insert(FoodEntity(
            id = entry.food.id,
            name = entry.food.name,
            brandName = entry.food.brandName,
            foodType = entry.food.foodType
        ))
        entry.food.servings.forEach { s ->
            servingDao.insert(ServingEntity(
                id = s.id,
                foodId = entry.food.id,
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
            ))
        }
    }
}

private suspend fun List<DiaryEntryEntity>.toDomainList(
    foodDao: FoodDao,
    servingDao: ServingDao
): List<DiaryEntry> {
    val foodIds = map { it.foodId }.distinct()
    val foods = foodDao.getByIds(foodIds)
    val servingsByFood = servingDao.getByFoodIds(foodIds).groupBy { it.foodId }
    val foodMap = foods.associate { f ->
        f.id to f.toDomain(servingsByFood[f.id]?.map { it.toDomain() } ?: emptyList())
    }
    return mapNotNull { e ->
        val food = foodMap[e.foodId] ?: return@mapNotNull null
        val serving = servingsByFood[e.foodId]?.find { it.id == e.servingId } ?: return@mapNotNull null
        DiaryEntry(
            id = e.id,
            date = e.date,
            mealType = MealType.valueOf(e.mealType),
            food = food,
            serving = serving.toDomain(),
            multiplier = e.multiplier
        )
    }
}

private fun FoodEntity.toDomain(servings: List<Serving>) = Food(
    id = id,
    name = name,
    brandName = brandName,
    foodType = foodType,
    servings = servings
)

private fun ServingEntity.toDomain() = Serving(
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
