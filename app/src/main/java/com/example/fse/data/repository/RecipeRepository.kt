package com.example.fse.data.repository

import com.example.fse.data.api.FatSecretClient
import com.example.fse.data.api.FatSecretDto
import com.example.fse.data.api.FatSecretProfileDto
import com.example.fse.data.local.SearchCache
import com.example.fse.domain.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

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

    private val myFavoritesCache = SearchCache<List<Recipe>>(ttlMs = 24 * 60 * 60 * 1000L)

    private val myFavoritesCacheKey = "my_recipe_favorites"

    private val favoritesLoadMutex = Mutex()

    fun triggerMyRecipesRefresh() { refreshTrigger.value++ }

    /**
     * Flow of user's favorite recipes from FatSecret (when connected via 3-legged OAuth).
     * Serves cached list first, then refreshes from the API. Mutex avoids cancelling in-flight requests on refresh.
     */
    fun getUserFavoriteRecipes(): Flow<List<Recipe>> = flow {
        combine(oauth1TokenStore.hasTokens, refreshTrigger) { hasTokens, _ -> hasTokens }
            .collect { hasTokens ->
                favoritesLoadMutex.withLock {
                    if (hasTokens) {
                        val cached = withContext(Dispatchers.IO) { myFavoritesCache.get(myFavoritesCacheKey) }
                        if (cached != null) emit(cached)
                        val recipes = withContext(Dispatchers.IO) {
                            getProfileApi()?.let { api ->
                                runCatching { api.getRecipeFavorites() }.getOrNull()?.body()
                                    ?.recipes?.recipeList(json)?.map { it.toRecipe() } ?: emptyList()
                            } ?: emptyList()
                        }
                        withContext(Dispatchers.IO) {
                            myFavoritesCache.put(myFavoritesCacheKey, recipes)
                        }
                        emit(recipes)
                    } else {
                        val stale = withContext(Dispatchers.IO) { myFavoritesCache.get(myFavoritesCacheKey) }
                        emit(stale ?: emptyList())
                    }
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

    suspend fun getRecipeById(recipeId: Long): Result<Recipe> = withContext(Dispatchers.IO) {
        runCatching {
            val tokens = oauth1TokenStore.getTokens()
            val api = if (tokens != null) {
                FatSecretClient.createPlatformApiWithOAuth1(tokens.first, tokens.second)
            } else {
                getApi() ?: throw Exception("Connect FatSecret in Account to load recipe details")
            }
            val response = api.getRecipeV2(recipeId)
            if (!response.isSuccessful) throw Exception("Recipe request failed: ${response.code()}")
            val wrapper = response.body() ?: throw Exception("Empty response")
            wrapper.error?.let { throw Exception(it.message ?: "API error ${it.code}") }
            val recipeEl = wrapper.recipe ?: throw Exception("No recipe in response")
            parseRecipeGetV2(recipeEl)
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
    types = emptyList(),
    recipeUrl = recipe_url
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

private val recipeDetailJson = FatSecretClient.json

private fun parseRecipeDirections(directionsEl: JsonElement?): List<String> {
    if (directionsEl == null) return emptyList()
    val dirObj = directionsEl as? JsonObject ?: return emptyList()
    val d = dirObj["direction"] ?: return emptyList()
    fun stepText(el: JsonElement): String? {
        val o = el as? JsonObject ?: return null
        return (o["direction_description"] as? JsonPrimitive)?.content
    }
    return when (d) {
        is JsonArray -> d.mapNotNull { stepText(it) }
        else -> listOfNotNull(stepText(d))
    }
}

private fun parseRecipeGetV2(el: JsonElement): Recipe {
    val dto = recipeDetailJson.decodeFromJsonElement(FatSecretDto.RecipeItemDto.serializer(), el)
    val base = dto.toRecipe()
    val obj = el as? JsonObject ?: return base
    val url = (obj["recipe_url"] as? JsonPrimitive)?.content ?: base.recipeUrl
    val directions = parseRecipeDirections(obj["directions"])
    return base.copy(recipeUrl = url, directions = directions)
}
