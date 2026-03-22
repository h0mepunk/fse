package com.example.fse.di

import com.example.fse.FSEApplication
import com.example.fse.data.auth.FatSecretOAuth1Flow
import com.example.fse.data.auth.OAuth1TokenStore
import com.example.fse.data.repository.FoodRepository
import com.example.fse.data.repository.MealsRepository
import com.example.fse.data.repository.RecipeRepository

data class AppContainer(
    val foodRepository: FoodRepository,
    val recipeRepository: RecipeRepository,
    val mealsRepository: MealsRepository,
    val normsStore: com.example.fse.data.local.LocalNormsStore,
    val auth: com.example.fse.data.auth.FatSecretAuth,
    val oauth1TokenStore: OAuth1TokenStore,
    val oauth1Flow: FatSecretOAuth1Flow
) {
    companion object {
        fun from(app: FSEApplication): AppContainer = AppContainer(
            foodRepository = app.foodRepository,
            recipeRepository = app.recipeRepository,
            mealsRepository = app.mealsRepository,
            normsStore = app.normsStore,
            auth = app.fatSecretAuth,
            oauth1TokenStore = app.oauth1TokenStore,
            oauth1Flow = app.oauth1Flow
        )
    }
}
