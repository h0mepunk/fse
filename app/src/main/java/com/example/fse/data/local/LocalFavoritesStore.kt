package com.example.fse.data.local

import kotlinx.coroutines.flow.Flow

interface LocalFavoritesStore {
    val foods: Flow<List<StoredFood>>
    suspend fun add(food: StoredFood)
    suspend fun remove(foodId: Long)
    suspend fun contains(foodId: Long): Boolean
}
