package com.example.fse.data.local

import com.example.fse.data.local.db.dao.FavoriteDao
import com.example.fse.data.local.db.dao.FoodDao
import com.example.fse.data.local.db.dao.ServingDao
import com.example.fse.data.local.db.entity.FoodEntity
import com.example.fse.data.local.db.entity.ServingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFavoritesStore(
    private val favoriteDao: FavoriteDao,
    private val foodDao: FoodDao,
    private val servingDao: ServingDao,
) : LocalFavoritesStore {
    override val foods: Flow<List<StoredFood>> = favoriteDao.getAll().map { favorites ->
        if (favorites.isEmpty()) emptyList()
        else {
            val foodIds = favorites.map { it.foodId }
            val foodEntities = foodDao.getByIds(foodIds)
            val servingsByFood = servingDao.getByFoodIds(foodIds).groupBy { it.foodId }
            foodEntities.map { f ->
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
        favoriteDao.insert(com.example.fse.data.local.db.entity.FavoriteEntity(foodId = food.id))
    }

    override suspend fun remove(foodId: Long) {
        favoriteDao.deleteByFoodId(foodId)
    }

    override suspend fun contains(foodId: Long): Boolean = favoriteDao.contains(foodId)
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
