package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import school_index.Adapter
import school_index.AdapterCategory
import school_index.School
import school_index.SchoolIndex

object SchoolRepository {

    // 定义需要在一级菜单中显示的教务类别
    private val RELEVANT_MENU_CATEGORIES = setOf(
        AdapterCategory.BACHELOR_AND_ASSOCIATE,
        AdapterCategory.POSTGRADUATE,
        AdapterCategory.GENERAL_TOOL
    )

    /**
     * 核心加载函数：仅从内部存储文件读取 Protobuf 索引。
     * 每次调用都会从磁盘加载最新数据。
     */
    private suspend fun loadIndex(context: Context): SchoolIndex? {

        return withContext(Dispatchers.IO) {

            val internalFile = File(context.filesDir, "repo/index/school_index.pb")

            if (!internalFile.exists()) {
                println("错误：Protobuf 索引文件未在内部存储中找到，路径为 ${internalFile.absolutePath}。请确保 initOfflineRepo() 已完成。")
                return@withContext null
            }

            try {
                internalFile.inputStream().use { stream ->
                    val index = SchoolIndex.parseFrom(stream)
                    return@withContext index
                }
            } catch (ioException: IOException) {
                ioException.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }


    /**
     * 【一级页面数据】获取经过类别过滤的学校列表。
     */
    suspend fun getSchools(context: Context): List<School> {
        val index = loadIndex(context) ?: return emptyList()

        // 1. 过滤：筛选出包含相关教务适配器的学校
        val filteredSchools = index.schoolsList.filter { school ->
            // 检查该学校的适配器列表中，是否存在任一个适配器类别在目标集合中
            school.adaptersList.any { adapter ->
                adapter.category in RELEVANT_MENU_CATEGORIES
            }
        }

        // 2. 排序：方便索引和显示
        return filteredSchools.sortedBy { it.initial.uppercase() + it.name }
    }

    /**
     * 【二级页面数据】根据学校 ID 获取其所有的适配器列表。
     */
    suspend fun getAdaptersForSchool(context: Context, schoolId: String): List<Adapter> {
        return withContext(Dispatchers.IO) {
            val index = loadIndex(context)

            // 查找对应的学校
            val school = index?.schoolsList?.find { it.id == schoolId }

            // 返回该学校下的所有适配器
            return@withContext school?.adaptersList ?: emptyList()
        }
    }

    /**
     * 辅助方法：通过 ID 获取单个学校对象
     */
    suspend fun getSchoolById(context: Context, id: String): School? {
        return withContext(Dispatchers.IO) {
            val index = loadIndex(context)
            // 查找对应的学校
            return@withContext index?.schoolsList?.find { it.id == id }
        }
    }
}