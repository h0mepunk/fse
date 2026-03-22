package com.example.fse.data.local

import com.example.fse.data.local.db.dao.FoodDao
import com.example.fse.data.local.db.dao.RecentFoodDao
import com.example.fse.data.local.db.dao.ServingDao
import com.example.fse.data.local.db.entity.FoodEntity
import com.example.fse.data.local.db.entity.RecentFoodEntity
import com.example.fse.data.local.db.entity.ServingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRecentFoodStore(
    private val recentFoodDao: RecentFoodDao,
    private val foodDao: FoodDao,
    private val servingDao: ServingDao,
) : LocalRecentFoodStore {
    override val foods: Flow<List<StoredFood>> = recentFoodDao.getAll().map { recents ->
        if (recents.isEmpty()) emptyList()
        else {
            val foodIds = recents.map { it.foodId }
            val foodEntities = foodDao.getByIds(foodIds)
            val servingsByFood = servingDao.getByFoodIds(foodIds).groupBy { it.foodId }
            recents.mapNotNull { r ->
                val f = foodEntities.find { it.id == r.foodId } ?: return@mapNotNull null
                val servings = servingsByFood[f.id] ?: emptyList()
                f.toStoredFood(servings.map { it.toStoredServing() })
            }
        }
    }

    override suspend fun add(food: StoredFood) {
        foodDao.insert(
            FoodEntity(
                id = food.id,
                name = food.name,
                brandName = food.brandName,
                foodType = food.foodType
            )
        )
        servingDao.insertAll(
            food.servings.map { s ->
                ServingEntity(
                    id = s.id,
                    foodId = food.id,
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
        )
        recentFoodDao.insert(RecentFoodEntity(foodId = food.id, addedAt = System.currentTimeMillis()))
    }
}

private fun FoodEntity.toStoredFood(servings: List<StoredServing>) = StoredFood(
    id = id,
    name = name,
    brandName = brandName,
    foodType = foodType,
    servings = servings
)

private fun ServingEntity.toStoredServing() = StoredServing(
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
