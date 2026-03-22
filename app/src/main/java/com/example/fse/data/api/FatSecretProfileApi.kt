package com.example.fse.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * FatSecret Profile API (requires OAuth 1.0 3-legged tokens).
 */
interface FatSecretProfileApi {

    @GET("rest/food-entries/v2")
    suspend fun getFoodEntries(
        @Query("date") dateInt: Int,
        @Query("format") format: String = "json"
    ): Response<FatSecretProfileDto.FoodEntriesWrapper>

    @GET("rest/food/favorites/v2")
    suspend fun getFavorites(
        @Query("format") format: String = "json"
    ): Response<FatSecretProfileDto.ProfileFoodsWrapper>

    @GET("rest/food/recently-eaten/v2")
    suspend fun getRecentlyEaten(
        @Query("format") format: String = "json"
    ): Response<FatSecretProfileDto.ProfileFoodsWrapper>

    @GET("rest/recipes/search/v3")
    suspend fun searchRecipes(
        @Query("search_expression") query: String,
        @Query("page_number") page: Int = 0,
        @Query("max_results") maxResults: Int = 20,
        @Query("format") format: String = "json"
    ): retrofit2.Response<com.example.fse.data.api.FatSecretDto.RecipesSearchWrapper>

    @GET("rest/recipe/favorites/v2")
    suspend fun getRecipeFavorites(
        @Query("format") format: String = "json"
    ): Response<FatSecretProfileDto.ProfileRecipesWrapper>

    @POST("rest/recipe/favorites/v1")
    suspend fun addRecipeFavorite(
        @Query("recipe_id") recipeId: Long,
        @Query("format") format: String = "json"
    ): Response<FatSecretProfileDto.SuccessResponse>

    @GET("rest/saved-meals/v2")
    suspend fun getSavedMeals(
        @Query("meal") meal: String? = null,
        @Query("format") format: String = "json"
    ): Response<FatSecretProfileDto.SavedMealsWrapper>

    @GET("rest/saved-meals/item/v2")
    suspend fun getSavedMealItems(
        @Query("saved_meal_id") savedMealId: Long,
        @Query("format") format: String = "json"
    ): Response<FatSecretProfileDto.SavedMealItemsWrapper>
}
