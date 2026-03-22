package com.example.fse.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fse.data.local.db.dao.DiaryDao
import com.example.fse.data.local.db.dao.FavoriteDao
import com.example.fse.data.local.db.dao.FoodDao
import com.example.fse.data.local.db.dao.NormDao
import com.example.fse.data.local.db.dao.RecipeDao
import com.example.fse.data.local.db.dao.RecentFoodDao
import com.example.fse.data.local.db.dao.ServingDao
import com.example.fse.data.local.db.entity.DiaryEntryEntity
import com.example.fse.data.local.db.entity.FavoriteEntity
import com.example.fse.data.local.db.entity.FoodEntity
import com.example.fse.data.local.db.entity.NormEntity
import com.example.fse.data.local.db.entity.RecipeEntity
import com.example.fse.data.local.db.entity.RecipeMetaEntity
import com.example.fse.data.local.db.entity.RecentFoodEntity
import com.example.fse.data.local.db.entity.ServingEntity

@Database(
    entities = [
        FoodEntity::class,
        ServingEntity::class,
        DiaryEntryEntity::class,
        FavoriteEntity::class,
        RecentFoodEntity::class,
        NormEntity::class,
        RecipeEntity::class,
        RecipeMetaEntity::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun servingDao(): ServingDao
    abstract fun diaryDao(): DiaryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentFoodDao(): RecentFoodDao
    abstract fun normDao(): NormDao
    abstract fun recipeDao(): RecipeDao
}

fun createAppDatabase(context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "fse.db")
        .fallbackToDestructiveMigration()
        .build()
