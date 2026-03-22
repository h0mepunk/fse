package com.example.fse.data.local

import com.example.fse.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface LocalRecipeStore {
    val lastQuery: Flow<String>
    val cachedRecipes: Flow<List<Recipe>>
    suspend fun saveRecipes(query: String, recipes: List<Recipe>)
}
