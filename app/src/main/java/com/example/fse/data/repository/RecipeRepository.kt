package com.example.fse.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi

import com.example.fse.data.api.FatSecretClient
import com.example.fse.data.api.FatSecretDto
import com.example.fse.data.api.FatSecretProfileDto
import com.example.fse.domain.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRepository(
    private val fatSecretAuth: com.example.fse.data.auth.FatSecretAuth,
    private val recipeStore: com.example.fse.data.local.LocalRecipeStore,
    private val oauth1TokenStore: com.example.fse.data.auth.OAuth1TokenStore
) {
    private suspend fun getApi(): com.example.fse.data.api.FatSecretApi? =
        fatSecretAuth.getAccessToken().getOrNull()?.let { fatSecretAuth.createApi(it) }

    private suspend fun getProfileApi(): com.example.fse.data.api.FatSecretProfileApi? =
        oauth1TokenStore.getTokens()?.let { (token, secret) ->
            FatSecretClient.createProfileApi(token, secret)
        }

    private val json = FatSecretClient.json

    private val refreshTrigger = MutableStateFlow(0)

    fun triggerMyRecipesRefresh() { refreshTrigger.value++ }

    /**
     * Flow of user's favorite recipes from FatSecret (when connected via 3-legged OAuth).
     * Empty when not connected.
     */
    fun getUserFavoriteRecipes(): Flow<List<Recipe>> = combine(
        oauth1TokenStore.hasTokens,
        refreshTrigger
    ) { hasTokens, _ -> hasTokens }
        .flatMapLatest { hasTokens ->
            flow {
                if (hasTokens) {
                    val recipes = withContext(Dispatchers.IO) {
                        getProfileApi()?.let { api ->
                            runCatching { api.getRecipeFavorites() }.getOrNull()?.body()
                                ?.recipes?.recipeList(json)?.map { it.toRecipe() } ?: emptyList()
                        } ?: emptyList()
                    }
                    emit(recipes)
                } else {
                    emit(emptyList())
                }
            }
        }

    suspend fun searchRecipes(query: String, page: Int = 0): Result<List<Recipe>> {
        val profileApi = getProfileApi()
        if (profileApi != null) {
            return runCatching {
                val response = profileApi.searchRecipes(query, page)
                if (!response.isSuccessful) throw Exception("Search failed: ${response.code()}")
                val wrapper = response.body() ?: throw Exception("Empty response")
                wrapper.error?.let { throw Exception(it.message ?: "API error ${it.code}") }
                val recipesResponse = wrapper.recipes ?: return@runCatching emptyList()
                val recipes = recipesResponse.recipeList(json).map { it.toRecipe() }
                recipeStore.saveRecipes(query, recipes)
                recipes
            }
        }
        val api = getApi() ?: return Result.failure(Exception("Connect FatSecret in Account to search recipes"))
        return runCatching {
            val response = api.searchRecipes(query, page)
            if (!response.isSuccessful) throw Exception("Search failed: ${response.code()}")
            val wrapper = response.body() ?: throw Exception("Empty response")
            wrapper.error?.let { throw Exception(it.message ?: "API error ${it.code}") }
            val recipesResponse = wrapper.recipes ?: return@runCatching emptyList()
            val recipes = recipesResponse.recipeList(json).map { it.toRecipe() }
            recipeStore.saveRecipes(query, recipes)
            recipes
        }
    }

    fun getCachedRecipes(): kotlinx.coroutines.flow.Flow<List<Recipe>> = recipeStore.cachedRecipes
    fun getLastQuery(): kotlinx.coroutines.flow.Flow<String> = recipeStore.lastQuery

    suspend fun addRecipeToFavorites(recipeId: Long): Result<Unit> {
        val api = getProfileApi() ?: return Result.failure(Exception("Connect FatSecret to add recipes"))
        return runCatching {
            val response = api.addRecipeFavorite(recipeId)
            if (!response.isSuccessful) throw Exception("Failed to add recipe: ${response.code()}")
            triggerMyRecipesRefresh()
            Unit
        }
    }
}

private fun FatSecretProfileDto.ProfileRecipeItem.toRecipe(): Recipe = Recipe(
    id = recipe_id.toLongOrNull() ?: 0L,
    name = recipe_name,
    description = recipe_description,
    imageUrl = recipe_image,
    calories = 0.0,
    protein = 0.0,
    carbs = 0.0,
    fat = 0.0,
    ingredients = emptyList(),
    types = emptyList()
)

private fun FatSecretDto.RecipeItemDto.toRecipe(): Recipe {
    fun parseStringList(e: JsonElement?): List<String> = when (e) {
        null -> emptyList()
        is JsonArray -> e.mapNotNull { (it as? JsonPrimitive)?.content }
        else -> (e as? JsonPrimitive)?.content?.let { listOf(it) } ?: emptyList()
    }
    return Recipe(
        id = recipe_id,
        name = recipe_name,
        description = recipe_description,
        imageUrl = recipe_image,
        calories = recipe_nutrition?.calories?.toDoubleOrNull() ?: 0.0,
        protein = recipe_nutrition?.protein?.toDoubleOrNull() ?: 0.0,
        carbs = recipe_nutrition?.carbohydrate?.toDoubleOrNull() ?: 0.0,
        fat = recipe_nutrition?.fat?.toDoubleOrNull() ?: 0.0,
        ingredients = parseStringList(recipe_ingredients?.ingredient),
        types = parseStringList(recipe_types?.recipe_type)
    )
}
