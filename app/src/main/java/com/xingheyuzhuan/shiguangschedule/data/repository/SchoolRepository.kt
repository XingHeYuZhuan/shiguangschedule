package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xingheyuzhuan.shiguangschedule.data.model.School
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object SchoolRepository {
    /**
     * 从 /files/repo 文件夹读取学校列表。
     * 每次调用都会从磁盘加载最新数据。
     */
    suspend fun getSchools(context: Context): List<School> {

        // 将文件读取和 JSON 解析切换到 IO 调度器上执行，避免阻塞调用线程。
        return withContext(Dispatchers.IO) {
            val dataFile = File(context.filesDir, "repo/schools.json")
            val jsonString: String

            try {
                if (!dataFile.exists()) {
                    return@withContext emptyList()
                }
                jsonString = dataFile.readText()
            } catch (ioException: IOException) {
                ioException.printStackTrace()
                return@withContext emptyList() // 发生错误时返回空列表
            }

            val gson = Gson()
            val listType = object : TypeToken<List<School>>() {}.type

            val schools = gson.fromJson<List<School>>(jsonString, listType)

            // 排序，方便索引和显示
            val sortedSchools = schools.sortedBy { it.initial.uppercase() + it.name }

            return@withContext sortedSchools
        }
    }

    suspend fun getSchoolById(context: Context, id: String): School? {

        // 确保在 IO 线程执行，因为 getSchools 会执行 IO 操作
        return withContext(Dispatchers.IO) {
            val allSchools = getSchools(context)
            allSchools.find { it.id == id }
        }
    }
}