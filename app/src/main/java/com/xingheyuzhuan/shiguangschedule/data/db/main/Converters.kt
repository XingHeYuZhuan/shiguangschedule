package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// 使用 Gson 库处理更复杂的类型转换
class Converters {

    @TypeConverter
    fun fromStringSet(setOfStrings: Set<String>?): String? {
        // 将 Set<String> 转换为 JSON 字符串
        return Gson().toJson(setOfStrings)
    }

    @TypeConverter
    fun toStringSet(data: String?): Set<String>? {
        // 将 JSON 字符串转换回 Set<String>
        val listType = object : TypeToken<Set<String>>() {}.type
        return Gson().fromJson(data, listType)
    }
}