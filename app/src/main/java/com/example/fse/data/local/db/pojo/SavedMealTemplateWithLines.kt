package com.example.fse.data.local.db.pojo

import androidx.room.Embedded
import androidx.room.Relation
import com.example.fse.data.local.db.entity.SavedMealTemplateEntity
import com.example.fse.data.local.db.entity.SavedMealTemplateLineEntity

data class SavedMealTemplateWithLines(
    @Embedded val template: SavedMealTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val lines: List<SavedMealTemplateLineEntity>
)
