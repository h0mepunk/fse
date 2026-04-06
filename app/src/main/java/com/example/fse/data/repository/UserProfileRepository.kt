package com.example.fse.data.repository

import com.example.fse.data.local.LocalNormsStore
import com.example.fse.data.local.db.dao.PeriodStartDao
import com.example.fse.data.local.db.dao.UserProfileDao
import com.example.fse.data.local.db.entity.PeriodStartEntity
import com.example.fse.data.local.db.entity.UserProfileEntity
import com.example.fse.domain.model.ActivityLevel
import com.example.fse.domain.model.Nutrient
import com.example.fse.domain.model.NutritionGoal
import com.example.fse.domain.model.UserSex
import com.example.fse.domain.nutrition.NutritionTargetsCalculator
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class UserProfileRepository(
    private val userProfileDao: UserProfileDao,
    private val periodStartDao: PeriodStartDao,
    private val normsStore: LocalNormsStore
) {

    fun observeProfile(): Flow<UserProfileEntity> =
        userProfileDao.observe()
            .map { it ?: UserProfileEntity() }
            .distinctUntilChanged()

    fun observePeriodEntities(): Flow<List<PeriodStartEntity>> =
        periodStartDao.observeAllDesc()

    suspend fun saveProfile(
        sex: UserSex,
        heightCm: Float?,
        weightKg: Float?,
        ageYears: Int?,
        activity: ActivityLevel,
        goal: NutritionGoal
    ) {
        userProfileDao.upsert(
            UserProfileEntity(
                id = 1,
                sex = sex.name,
                heightCm = heightCm,
                weightKg = weightKg,
                ageYears = ageYears,
                activityMultiplier = activity.multiplier.toFloat(),
                goal = goal.name
            )
        )
    }

    suspend fun addPeriodStart(date: LocalDate) {
        periodStartDao.insert(PeriodStartEntity(startDate = date.toString()))
    }

    suspend fun removePeriodStart(id: Long) {
        periodStartDao.deleteById(id)
    }

    suspend fun applyCalculatedNormsToStore(): Boolean {
        val profile = userProfileDao.getOne() ?: UserProfileEntity()
        val sex = runCatching { UserSex.valueOf(profile.sex) }.getOrDefault(UserSex.UNSPECIFIED)
        val h = profile.heightCm?.toDouble() ?: return false
        val w = profile.weightKg?.toDouble() ?: return false
        val age = profile.ageYears ?: return false
        val mult = profile.activityMultiplier.toDouble()
        val activity = ActivityLevel.entries.minByOrNull { abs(it.multiplier - mult) }
            ?: ActivityLevel.MODERATE
        val goal = runCatching { NutritionGoal.valueOf(profile.goal) }.getOrDefault(NutritionGoal.MAINTENANCE)
        val r = NutritionTargetsCalculator.calculate(sex, w, h, age, activity, goal)
        normsStore.setCustomNorm(Nutrient.Calories, r.calories)
        normsStore.setCustomNorm(Nutrient.Protein, r.proteinG)
        normsStore.setCustomNorm(Nutrient.Carbs, r.carbsG)
        normsStore.setCustomNorm(Nutrient.Fat, r.fatG)
        return true
    }
}
