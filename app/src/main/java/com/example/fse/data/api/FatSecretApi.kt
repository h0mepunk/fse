package com.example.fse.data.api

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * FatSecret Platform REST API.
 * @see <a href="https://platform.fatsecret.com/docs/guides">FatSecret API Docs</a>
 */
interface FatSecretApi {

    /** v5 - may require OAuth 2.0 / Premier. Use v1 when OAuth 1.0 returns "api not resolved". */
    @GET("rest/foods/search/v5")
    suspend fun searchFoodsV5(
        @Query("search_expression") query: String,
        @Query("page_number") page: Int = 0,
        @Query("max_results") maxResults: Int = 20,
        @Query("format") format: String = "json"
    ): Response<FatSecretDto.FoodsSearchWrapper>

    /** v1 - works with OAuth 1.0 (3-legged). Returns minimal data; use food.get for nutrition. */
    @GET("rest/foods/search/v1")
    suspend fun searchFoodsV1(
        @Query("search_expression") query: String,
        @Query("page_number") page: Int = 0,
        @Query("max_results") maxResults: Int = 20,
        @Query("format") format: String = "json"
    ): Response<FatSecretDto.FoodsSearchWrapperV1>

    @GET("rest/food/v5")
    suspend fun getFood(
        @Query("food_id") foodId: Long,
        @Query("format") format: String = "json"
    ): Response<FatSecretDto.FoodGetWrapper>

    @GET("rest/recipes/search/v3")
    suspend fun searchRecipes(
        @Query("search_expression") query: String,
        @Query("page_number") page: Int = 0,
        @Query("max_results") maxResults: Int = 20,
        @Query("format") format: String = "json"
    ): Response<FatSecretDto.RecipesSearchWrapper>
}

/**
 * FatSecret OAuth 2.0 token endpoint.
 * NOTE: FatSecret requires token requests from whitelisted IPs (typically a backend).
 * For dev: use a BFF or add your dev machine IP in FatSecret dashboard.
 */
interface FatSecretOAuthApi {

    @FormUrlEncoded
    @POST("connect/token")
    suspend fun getToken(
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("scope") scope: String = "basic"
    ): retrofit2.Response<FatSecretDto.TokenResponse>
}
