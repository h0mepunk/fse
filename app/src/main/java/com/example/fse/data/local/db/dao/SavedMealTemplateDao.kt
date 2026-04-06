package com.example.fse.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.fse.data.local.db.entity.SavedMealTemplateEntity
import com.example.fse.data.local.db.entity.SavedMealTemplateLineEntity
import com.example.fse.data.local.db.pojo.SavedMealTemplateWithLines
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMealTemplateDao {

    @Transaction
    @Query("SELECT * FROM saved_meal_template ORDER BY id DESC")
    fun observeAllWithLines(): Flow<List<SavedMealTemplateWithLines>>

    @Transaction
    @Query("SELECT * FROM saved_meal_template WHERE id = :id")
    suspend fun getWithLines(id: Long): SavedMealTemplateWithLines?

    @Insert
    suspend fun insertTemplate(entity: SavedMealTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<SavedMealTemplateLineEntity>)

    @Query("DELETE FROM saved_meal_template_line WHERE templateId = :templateId")
    suspend fun deleteLinesForTemplate(templateId: Long)

    @Query("DELETE FROM saved_meal_template WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("UPDATE saved_meal_template SET name = :name WHERE id = :id")
    suspend fun updateTemplateName(id: Long, name: String)

    @Query(
        """
        UPDATE saved_meal_template_line SET
            multiplier = :multiplier,
            lineCalories = :lineCalories,
            lineProtein = :lineProtein,
            lineCarbs = :lineCarbs,
            lineFat = :lineFat
        WHERE id = :lineId AND templateId = :templateId
        """
    )
    suspend fun updateLineAmounts(
        lineId: Long,
        templateId: Long,
        multiplier: Double,
        lineCalories: Double,
        lineProtein: Double,
        lineCarbs: Double,
        lineFat: Double
    )

    @Query("DELETE FROM saved_meal_template_line WHERE id = :lineId AND templateId = :templateId")
    suspend fun deleteLine(lineId: Long, templateId: Long)
}
