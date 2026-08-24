package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.xingheyuzhuan.shiguangschedule.data.model.SchoolHistoryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import school_index.Adapter
import school_index.AdapterCategory
import school_index.School
import school_index.SchoolIndex
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * 将随应用发布的适配器叠加到可在线更新的仓库索引上。
 * 同 ID 的内置适配器优先，远程索引中的其他学校和适配器保持原顺序。
 */
internal fun mergeSchoolIndexes(primary: SchoolIndex, builtIn: SchoolIndex): SchoolIndex {
    val schools = linkedMapOf<String, School>()
    primary.schools.forEach { school -> schools[school.id] = school }

    builtIn.schools.forEach { builtInSchool ->
        val primarySchool = schools[builtInSchool.id]
        if (primarySchool == null) {
            schools[builtInSchool.id] = builtInSchool
        } else {
            val adapters = linkedMapOf<String, Adapter>()
            primarySchool.adapters.forEach { adapter -> adapters[adapter.adapter_id] = adapter }
            builtInSchool.adapters.forEach { adapter -> adapters[adapter.adapter_id] = adapter }

            schools[builtInSchool.id] = School(
                id = builtInSchool.id.ifBlank { primarySchool.id },
                name = builtInSchool.name.ifBlank { primarySchool.name },
                initial = builtInSchool.initial.ifBlank { primarySchool.initial },
                resource_folder = builtInSchool.resource_folder.ifBlank { primarySchool.resource_folder },
                adapters = adapters.values.toList(),
                unknownFields = primarySchool.unknownFields
            )
        }
    }

    return SchoolIndex(
        protocol_version = maxOf(primary.protocol_version, builtIn.protocol_version),
        version_id = primary.version_id.ifBlank { builtIn.version_id },
        schools = schools.values.toList(),
        unknownFields = primary.unknownFields
    )
}

/**
 * 学校数据仓库。
 * 职责：处理内部存储中 Protobuf 学校索引文件的读取与解析。
 */
@Single
class SchoolRepository(
    private val fileSystem: FileSystem,
    @Named("FilesDir") private val filesDir: Path
) {

    // 定义需要在一级菜单中显示的教务类别
    private val RELEVANT_MENU_CATEGORIES = setOf(
        AdapterCategory.BACHELOR_AND_ASSOCIATE,
        AdapterCategory.POSTGRADUATE,
        AdapterCategory.GENERAL_TOOL
    )

    /**
     * 核心加载函数：仅从内部存储文件读取 Protobuf 索引。
     */
    private suspend fun loadIndex(): SchoolIndex? {
        return withContext(Dispatchers.IO) {
            val indexDirectory = filesDir / "repo/index"
            val internalPath = indexDirectory / "school_index.pb"
            val builtInPath = indexDirectory / "builtin_school_index.pb"

            if (!fileSystem.exists(internalPath)) {
                println("错误：Protobuf 索引文件未找到: $internalPath")
                return@withContext null
            }

            try {
                val primaryIndex = fileSystem.source(internalPath).use { source ->
                    SchoolIndex.ADAPTER.decode(source.buffer())
                }

                if (!fileSystem.exists(builtInPath)) return@withContext primaryIndex

                val builtInIndex = runCatching {
                    fileSystem.source(builtInPath).use { source ->
                        SchoolIndex.ADAPTER.decode(source.buffer())
                    }
                }.onFailure { error ->
                    println("警告：内置学校索引解析失败，将仅使用远程索引: ${error.message}")
                }.getOrNull()

                if (builtInIndex == null) primaryIndex else mergeSchoolIndexes(primaryIndex, builtInIndex)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 【一级页面数据】获取经过类别过滤的学校列表。
     */
    suspend fun getSchools(): List<School> {
        val index = loadIndex() ?: return emptyList()

        // 1. 过滤：使用 Wire 生成的直接列表属性名
        val filteredSchools = index.schools.filter { school ->
            school.adapters.any { adapter ->
                adapter.category in RELEVANT_MENU_CATEGORIES
            }
        }

        // 2. 排序：initial 字段在 Wire 中保留了 proto 定义的原样
        return filteredSchools.sortedBy { it.initial.uppercase() + it.name }
    }

    /**
     * 【二级页面数据】根据学校 ID 获取其所有的适配器列表。
     */
    suspend fun getAdaptersForSchool(schoolId: String): List<Adapter> {
        return withContext(Dispatchers.IO) {
            val index = loadIndex()
            val school = index?.schools?.find { it.id == schoolId }
            return@withContext school?.adapters ?: emptyList()
        }
    }

    /**
     * 辅助方法：通过 ID 获取单个学校对象
     */
    suspend fun getSchoolById(id: String): School? {
        return withContext(Dispatchers.IO) {
            val index = loadIndex()
            return@withContext index?.schools?.find { it.id == id }
        }
    }
}


/**
 * 用户记录仓库
 *
 */
@Single
class SchoolHistoryRepository(
    @Named("SchoolHistory") private val dataStore: DataStore<Preferences>
) {
    val historyFlow: Flow<SchoolHistoryModel> = dataStore.data.map { prefs ->
        SchoolHistoryModel.fromPreferences(prefs)
    }

    /**
     * 保存上次选择的学校
     * 适配点：resourceFolder -> resource_folder
     */
    suspend fun saveLastSchool(category: AdapterCategory, school: School) {
        dataStore.edit { prefs ->
            val keys = SchoolHistoryModel.getKeysForCategory(category)
            prefs[keys.first] = school.id
            prefs[keys.second] = school.name
            prefs[keys.third] = school.resource_folder
        }
    }

    /**
     * 清除历史记录
     */
    suspend fun clearHistory(category: AdapterCategory) {
        dataStore.edit { prefs ->
            val keys = SchoolHistoryModel.getKeysForCategory(category)
            prefs.remove(keys.first)
            prefs.remove(keys.second)
            prefs.remove(keys.third)
        }
    }
}
