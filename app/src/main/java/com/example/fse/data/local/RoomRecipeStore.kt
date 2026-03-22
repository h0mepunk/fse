package com.example.fse.data.local

import com.example.fse.data.local.db.dao.RecipeDao
import com.example.fse.data.local.db.entity.RecipeEntity
import com.example.fse.data.local.db.entity.RecipeMetaEntity
import com.example.fse.domain.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRecipeStore(private val recipeDao: RecipeDao) : LocalRecipeStore {

    override val lastQuery: Flow<String> = recipeDao.getMeta("last_query").map {
        it?.value ?: ""
    }

    override val cachedRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun saveRecipes(query: String, recipes: List<Recipe>) {
        recipeDao.insertMeta(RecipeMetaEntity(key = "last_query", value = query))
        recipeDao.deleteAll()
        recipeDao.insertAll(recipes.map { it.toEntity() })
    }
}

private fun RecipeEntity.toDomain() = Recipe(
    id = id,
    name = name,
    description = description,
    imageUrl = imageUrl,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    ingredients = ingredients,
    types = types
)

private fun Recipe.toEntity() = RecipeEntity(
    id = id,
    name = name,
    description = description,
    imageUrl = imageUrl,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    ingredients = ingredients,
    types = types
)
