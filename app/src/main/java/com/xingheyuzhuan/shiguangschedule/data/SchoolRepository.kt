// com/xingheyuzhuan/shiguangschedule/data/SchoolRepository.kt
package com.xingheyuzhuan.shiguangschedule.data

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.model.School
import kotlinx.serialization.json.Json
import java.io.IOException

// 导入协程相关的包
import kotlinx.coroutines.Dispatchers // 用于指定协程运行的调度器
import kotlinx.coroutines.withContext // 用于切换协程上下文

object SchoolRepository {

    private var cachedSchools: List<School>? = null

    // 从 assets 文件夹读取学校列表
    suspend fun getSchools(context: Context): List<School> {
        // 如果有缓存，直接返回，这部分不会阻塞
        if (cachedSchools != null) {
            return cachedSchools!!
        }

        // 使用 withContext(Dispatchers.IO) 将文件读取和 JSON 解析这种耗时操作
        // 切换到 IO 调度器（一个专门处理 IO 任务的线程池）上执行。
        // 这样，调用 getSchools 的线程（例如主线程）就不会被阻塞。
        return withContext(Dispatchers.IO) {
            val jsonString: String
            try {
                jsonString = context.assets.open("schools.json").bufferedReader().use { it.readText() }
            } catch (ioException: IOException) {
                ioException.printStackTrace()

                return@withContext emptyList() // 发生错误时返回空列表
            }

            val schools = Json.decodeFromString<List<School>>(jsonString)
            cachedSchools = schools.sortedBy { it.initial.uppercase() + it.name } // 排序，方便索引和显示
            return@withContext cachedSchools!! // 返回加载并排序后的学校列表
        }
    }

    suspend fun getSchoolById(context: Context, id: String): School? {
        // 首先尝试从缓存中查找
        cachedSchools?.let { schools ->
            return schools.find { it.id == id }
        }

        // 如果缓存为空，则加载所有学校并再次查找
        // 确保在 IO 线程执行，因为 getSchools 会执行 IO 操作
        return withContext(Dispatchers.IO) {
            val allSchools = getSchools(context) // 调用挂起函数
            allSchools.find { it.id == id }
        }
    }
}