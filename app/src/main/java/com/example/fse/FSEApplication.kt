package com.example.fse

import android.app.Application
import com.example.fse.data.auth.FatSecretAuth
import com.example.fse.data.auth.FatSecretOAuth1Flow
import com.example.fse.data.auth.OAuth1TokenStore
import com.example.fse.data.auth.TokenStore
import com.example.fse.data.local.RoomDiaryStore
import com.example.fse.data.local.RoomFavoritesStore
import com.example.fse.data.local.RoomNormsStore
import com.example.fse.data.local.RoomRecipeStore
import com.example.fse.data.local.RoomRecentFoodStore
import com.example.fse.data.local.db.createAppDatabase
import com.example.fse.data.repository.FoodRepository
import com.example.fse.data.repository.RecipeRepository
import com.example.fse.data.repository.SavedMealTemplateRepository
import com.example.fse.data.repository.SavedMealsRepository
import com.example.fse.data.repository.UserProfileRepository

class FSEApplication : Application() {

    val tokenStore by lazy { TokenStore(this) }
    val fatSecretAuth by lazy { FatSecretAuth(tokenStore) }
    val oauth1TokenStore by lazy { OAuth1TokenStore(this) }
    val oauth1Flow by lazy { FatSecretOAuth1Flow(oauth1TokenStore) }

    private val db by lazy { createAppDatabase(this) }

    val recentFoodStore by lazy {
        RoomRecentFoodStore(
            recentFoodDao = db.recentFoodDao(),
            foodDao = db.foodDao(),
            servingDao = db.servingDao()
        )
    }
    val diaryStore by lazy {
        RoomDiaryStore(
            diaryDao = db.diaryDao(),
            foodDao = db.foodDao(),
            servingDao = db.servingDao()
        )
    }
    val favoritesStore by lazy {
        RoomFavoritesStore(
            favoriteDao = db.favoriteDao(),
            foodDao = db.foodDao(),
            servingDao = db.servingDao()
        )
    }
    val recipeStore by lazy {
        RoomRecipeStore(recipeDao = db.recipeDao())
    }
    val normsStore by lazy { RoomNormsStore(normDao = db.normDao()) }

    val foodRepository by lazy {
        FoodRepository(
            fatSecretAuth = fatSecretAuth,
            recentStore = recentFoodStore,
            diaryStore = diaryStore,
            favoritesStore = favoritesStore,
            oauth1TokenStore = oauth1TokenStore
        )
    }

    val recipeRepository by lazy {
        RecipeRepository(
            fatSecretAuth = fatSecretAuth,
            recipeStore = recipeStore,
            oauth1TokenStore = oauth1TokenStore
        )
    }

    val savedMealsRepository by lazy {
        SavedMealsRepository(oauth1TokenStore = oauth1TokenStore)
    }

    val savedMealTemplateRepository by lazy {
        SavedMealTemplateRepository(
            dao = db.savedMealTemplateDao(),
            foodRepository = foodRepository
        )
    }

    val userProfileRepository by lazy {
        UserProfileRepository(
            userProfileDao = db.userProfileDao(),
            periodStartDao = db.periodStartDao(),
            normsStore = normsStore
        )
    }
}
