package com.example.fse.data.local

import kotlinx.coroutines.flow.Flow

interface LocalRecentFoodStore {
    val foods: Flow<List<StoredFood>>
    suspend fun add(food: StoredFood)
}
