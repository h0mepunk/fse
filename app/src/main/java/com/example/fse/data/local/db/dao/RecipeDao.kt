package com.example.fse.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fse.data.local.db.entity.RecipeEntity
import com.example.fse.data.local.db.entity.RecipeMetaEntity

@Dao
interface RecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<RecipeEntity>)

    @Query("DELETE FROM recipe")
    suspend fun deleteAll()

    @Query("SELECT * FROM recipe ORDER BY id")
    fun getAllRecipes(): kotlinx.coroutines.flow.Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: RecipeMetaEntity)

    @Query("SELECT * FROM recipe_meta WHERE `key` = :key LIMIT 1")
    fun getMeta(key: String): kotlinx.coroutines.flow.Flow<RecipeMetaEntity?>
}
