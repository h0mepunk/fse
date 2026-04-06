package com.example.fse.data.repository

import com.example.fse.data.local.db.dao.SavedMealTemplateDao
import com.example.fse.data.local.db.entity.SavedMealTemplateEntity
import com.example.fse.data.local.db.entity.SavedMealTemplateLineEntity
import com.example.fse.data.local.db.pojo.SavedMealTemplateWithLines
import com.example.fse.domain.model.DiaryEntry
import com.example.fse.domain.model.Food
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.Serving
import com.example.fse.domain.model.UserMealTemplate
import com.example.fse.domain.model.UserMealTemplateLine
import com.example.fse.ui.format.extractGrams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SavedMealTemplateRepository(
    private val dao: SavedMealTemplateDao,
    private val foodRepository: FoodRepository
) {

    fun observeTemplates(): Flow<List<UserMealTemplate>> =
        dao.observeAllWithLines().map { list -> list.map { it.toDomain() } }

    suspend fun createFromDiary(name: String, entries: List<DiaryEntry>): Long = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Название не может быть пустым" }
        val templateId = dao.insertTemplate(SavedMealTemplateEntity(name = trimmed))
        val lines = entries.map { e ->
            lineEntity(
                templateId = templateId,
                foodId = e.food.id,
                servingId = e.serving.id,
                foodName = e.food.name,
                brandName = e.food.brandName,
                servingDescription = e.serving.description,
                multiplier = e.multiplier,
                serving = e.serving
            )
        }
        if (lines.isNotEmpty()) dao.insertLines(lines)
        templateId
    }

    suspend fun addLineFromSearch(
        templateId: Long,
        food: Food,
        serving: Serving,
        multiplier: Double
    ): Unit = withContext(Dispatchers.IO) {
        val mult = multiplier.coerceAtLeast(0.01)
        dao.insertLines(
            listOf(
                lineEntity(
                    templateId = templateId,
                    foodId = food.id,
                    servingId = serving.id,
                    foodName = food.name,
                    brandName = food.brandName,
                    servingDescription = serving.description,
                    multiplier = mult,
                    serving = serving
                )
            )
        )
    }

    suspend fun updateLineMultiplier(line: UserMealTemplateLine, multiplierText: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val newMult = multiplierText.replace(',', '.').trim().toDoubleOrNull()?.coerceAtLeast(0.01)
                ?: return@withContext Result.failure(Exception("Некорректный множитель"))
            val ratio = newMult / line.multiplier
            dao.updateLineAmounts(
                lineId = line.id,
                templateId = line.templateId,
                multiplier = newMult,
                lineCalories = line.lineCalories * ratio,
                lineProtein = line.lineProtein * ratio,
                lineCarbs = line.lineCarbs * ratio,
                lineFat = line.lineFat * ratio
            )
            Result.success(Unit)
        }

    suspend fun updateLineGrams(line: UserMealTemplateLine, gramsText: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val baseGrams = extractGrams(line.servingDescription)
                ?: return@withContext Result.failure(Exception("Для этой порции нет граммов в описании — измените множитель в поиске"))
            val newGrams = gramsText.replace(',', '.').trim().toDoubleOrNull()
                ?: return@withContext Result.failure(Exception("Некорректное число граммов"))
            if (newGrams <= 0) return@withContext Result.failure(Exception("Граммы должны быть > 0"))
            val newMult = (newGrams / baseGrams).coerceAtLeast(0.01)
            val unitC = line.lineCalories / line.multiplier
            val unitP = line.lineProtein / line.multiplier
            val unitCarb = line.lineCarbs / line.multiplier
            val unitF = line.lineFat / line.multiplier
            dao.updateLineAmounts(
                lineId = line.id,
                templateId = line.templateId,
                multiplier = newMult,
                lineCalories = unitC * newMult,
                lineProtein = unitP * newMult,
                lineCarbs = unitCarb * newMult,
                lineFat = unitF * newMult
            )
            Result.success(Unit)
        }

    suspend fun deleteLine(lineId: Long, templateId: Long) = withContext(Dispatchers.IO) {
        dao.deleteLine(lineId, templateId)
    }

    suspend fun deleteTemplate(templateId: Long) = withContext(Dispatchers.IO) {
        dao.deleteTemplate(templateId)
    }

    suspend fun renameTemplate(templateId: Long, name: String) = withContext(Dispatchers.IO) {
        val n = name.trim()
        require(n.isNotEmpty())
        dao.updateTemplateName(templateId, n)
    }

    suspend fun applyToDiary(
        templateId: Long,
        date: LocalDate,
        mealType: MealType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bundle = dao.getWithLines(templateId) ?: throw Exception("Шаблон не найден")
            for (line in bundle.lines) {
                val food = foodRepository.getFood(line.foodId).getOrThrow()
                val serving = food.servings.find { it.id == line.servingId }
                    ?: food.servings.firstOrNull()
                    ?: throw Exception("Нет порции для ${line.foodName}")
                foodRepository.addToDiary(
                    FoodRepository.AddToDiaryRequest(
                        date = date,
                        mealType = mealType,
                        food = food,
                        serving = serving,
                        multiplier = line.multiplier
                    )
                )
            }
        }
    }

    private fun lineEntity(
        templateId: Long,
        foodId: Long,
        servingId: Long,
        foodName: String,
        brandName: String?,
        servingDescription: String,
        multiplier: Double,
        serving: Serving
    ) = SavedMealTemplateLineEntity(
        templateId = templateId,
        foodId = foodId,
        servingId = servingId,
        foodName = foodName,
        brandName = brandName,
        servingDescription = servingDescription,
        multiplier = multiplier,
        lineCalories = serving.calories * multiplier,
        lineProtein = serving.protein * multiplier,
        lineCarbs = serving.carbs * multiplier,
        lineFat = serving.fat * multiplier
    )

    private fun SavedMealTemplateWithLines.toDomain(): UserMealTemplate = UserMealTemplate(
        id = template.id,
        name = template.name,
        lines = lines.sortedBy { it.id }.map { it.toDomain() }
    )

    private fun SavedMealTemplateLineEntity.toDomain() = UserMealTemplateLine(
        id = id,
        templateId = templateId,
        foodId = foodId,
        servingId = servingId,
        foodName = foodName,
        brandName = brandName,
        servingDescription = servingDescription,
        multiplier = multiplier,
        lineCalories = lineCalories,
        lineProtein = lineProtein,
        lineCarbs = lineCarbs,
        lineFat = lineFat
    )
}
