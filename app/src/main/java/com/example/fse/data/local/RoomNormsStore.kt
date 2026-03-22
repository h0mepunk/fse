package com.example.fse.data.local

import com.example.fse.data.local.db.dao.NormDao
import com.example.fse.data.local.db.entity.NormEntity
import com.example.fse.domain.model.Nutrient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNormsStore(private val normDao: NormDao) : LocalNormsStore {

    override fun getCustomNorm(nutrient: Nutrient): Flow<Double?> = normDao.get(nutrient.name).map {
        it?.value?.takeIf { v -> v > 0 }
    }

    override fun getAllCustomNorms(): Flow<Map<Nutrient, Double>> = normDao.getAllCustom().map { entities ->
        entities.mapNotNull { e ->
            Nutrient.entries.find { it.name == e.nutrient }?.let { n ->
                n to e.value
            }
        }.toMap()
    }

    override suspend fun setCustomNorm(nutrient: Nutrient, value: Double) {
        if (value > 0) {
            normDao.insert(NormEntity(nutrient = nutrient.name, value = value))
        } else {
            normDao.delete(nutrient.name)
        }
    }

    override suspend fun resetToDefaults() {
        normDao.deleteAll()
    }
}
