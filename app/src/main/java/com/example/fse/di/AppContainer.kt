package com.example.fse.di

import com.example.fse.FSEApplication
import com.example.fse.data.auth.FatSecretOAuth1Flow
import com.example.fse.data.auth.OAuth1TokenStore
import com.example.fse.data.repository.FoodRepository
import com.example.fse.data.repository.RecipeRepository
import com.example.fse.data.repository.SavedMealTemplateRepository
import com.example.fse.data.repository.SavedMealsRepository
import com.example.fse.data.repository.UserProfileRepository
import com.example.fse.domain.model.PendingMealTemplateFromDiary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer(
    val foodRepository: FoodRepository,
    val recipeRepository: RecipeRepository,
    val savedMealsRepository: SavedMealsRepository,
    val savedMealTemplateRepository: SavedMealTemplateRepository,
    val normsStore: com.example.fse.data.local.LocalNormsStore,
    val auth: com.example.fse.data.auth.FatSecretAuth,
    val oauth1TokenStore: OAuth1TokenStore,
    val oauth1Flow: FatSecretOAuth1Flow,
    val userProfileRepository: UserProfileRepository
) {
    private val _pendingMealTemplateFromDiary = MutableStateFlow<PendingMealTemplateFromDiary?>(null)
    val pendingMealTemplateFromDiary: StateFlow<PendingMealTemplateFromDiary?> =
        _pendingMealTemplateFromDiary.asStateFlow()

    fun offerMealTemplateFromDiary(pending: PendingMealTemplateFromDiary) {
        _pendingMealTemplateFromDiary.value = pending
    }

    fun clearMealTemplateFromDiary() {
        _pendingMealTemplateFromDiary.value = null
    }

    companion object {
        fun from(app: FSEApplication): AppContainer = AppContainer(
            foodRepository = app.foodRepository,
            recipeRepository = app.recipeRepository,
            savedMealsRepository = app.savedMealsRepository,
            savedMealTemplateRepository = app.savedMealTemplateRepository,
            normsStore = app.normsStore,
            auth = app.fatSecretAuth,
            oauth1TokenStore = app.oauth1TokenStore,
            oauth1Flow = app.oauth1Flow,
            userProfileRepository = app.userProfileRepository
        )
    }
}
