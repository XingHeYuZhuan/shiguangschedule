package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun fromStringSet(setOfStrings: Set<String>?): String? {
        if (setOfStrings == null) return null
        return Json.encodeToString(setOfStrings)
    }

    @TypeConverter
    fun toStringSet(data: String?): Set<String>? {
        if (data == null) return null
        return try {
            Json.decodeFromString<Set<String>>(data)
        } catch (e: Exception) {
            null
        }
    }
}