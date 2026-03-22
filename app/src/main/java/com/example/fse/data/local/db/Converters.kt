package com.example.fse.data.local.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

object Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val stringListSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(s: String?): LocalDate? = s?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? =
        list?.let { json.encodeToString(stringListSerializer, it) }

    @TypeConverter
    fun toStringList(s: String?): List<String>? =
        s?.let { json.decodeFromString(stringListSerializer, it) }
}
