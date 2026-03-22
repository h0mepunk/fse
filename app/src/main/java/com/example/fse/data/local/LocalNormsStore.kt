package com.example.fse.data.local

import com.example.fse.domain.model.Nutrient
import kotlinx.coroutines.flow.Flow

interface LocalNormsStore {
    fun getCustomNorm(nutrient: Nutrient): Flow<Double?>
    fun getAllCustomNorms(): Flow<Map<Nutrient, Double>>
    suspend fun setCustomNorm(nutrient: Nutrient, value: Double)
    suspend fun resetToDefaults()
}
