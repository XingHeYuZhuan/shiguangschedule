package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import androidx.room.Transaction
import com.xingheyuzhuan.shiguangschedule.BuildConfig
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableDao
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.CourseTableImportModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.ImportCourseJsonModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.SingleTablePack
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.TotalAppBackupEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

/**
 * 模块化备份定义
 */
enum class BackupModule(val key: String) {
    COURSE("course"),
    STYLE("style")
}

/**
 * 各模块的物理隔离载体
 */
data class AppBackupPackage(
    val meta: BackupMeta,
    val payloadMap: Map<String, ByteArray>
)

@Serializable
data class BackupMeta(
    val backupTimestamp: Long,
    val appVersionCode: Int,
    val appVersionName: String,
    val modules: List<ModuleInfo>
)

@Serializable
data class ModuleInfo(
    val key: String,
    val schemaVersion: Int
)

/**
 * 备份与恢复的中央总仓库
 * 职责：调度各业务模块的原子化备份与恢复，确保全软件数据的一致性与扩展性。
 */
@Single
class BackupRepository(
    private val context: Context,
    private val courseTableDao: CourseTableDao,
    private val courseTableRepository: CourseTableRepository,
    private val courseConversionRepository: CourseConversionRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val styleSettingsRepository: StyleSettingsRepository
) {

    /**
     * 构建全软件多模块统一内存备份包
     */
    suspend fun createFullSoftwareBackup(modules: List<BackupModule>): AppBackupPackage? = withContext(Dispatchers.IO) {
        try {
            val payloadMap = mutableMapOf<String, ByteArray>()
            val moduleInfos = mutableListOf<ModuleInfo>()

            modules.forEach { module ->
                when (module) {
                    BackupModule.COURSE -> {
                        exportAllCourseTablesCbor()?.let {
                            payloadMap[module.key] = it
                            moduleInfos.add(ModuleInfo(module.key, CourseImportExport.COURSE_SCHEMA_VERSION))
                        }
                    }
                    BackupModule.STYLE -> {
                        exportAppStyleBytes()?.let {
                            payloadMap[module.key] = it
                            moduleInfos.add(ModuleInfo(module.key, StyleSettingsRepository.STYLE_SCHEMA_VERSION))
                        }
                    }
                }
            }

            if (payloadMap.isEmpty()) return@withContext null

            AppBackupPackage(
                meta = BackupMeta(
                    backupTimestamp = System.currentTimeMillis(),
                    appVersionCode = BuildConfig.VERSION_CODE,
                    appVersionName = BuildConfig.VERSION_NAME,
                    modules = moduleInfos
                ),
                payloadMap = payloadMap
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 原子化分发恢复网关
     */
    @Transaction
    suspend fun restoreFullSoftwareBackup(backupPackage: AppBackupPackage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            backupPackage.meta.modules.forEach { info ->
                when (info.key) {
                    BackupModule.COURSE.key -> {
                        if (info.schemaVersion > CourseImportExport.COURSE_SCHEMA_VERSION) {
                            return@withContext Result.failure(IllegalStateException(context.getString(R.string.backup_err_version_too_new)))
                        }
                    }
                    BackupModule.STYLE.key -> {
                        if (info.schemaVersion > StyleSettingsRepository.STYLE_SCHEMA_VERSION) {
                            return@withContext Result.failure(IllegalStateException(context.getString(R.string.backup_err_version_too_new)))
                        }
                    }
                }
            }
            backupPackage.meta.modules.forEach { info ->
                val data = backupPackage.payloadMap[info.key] ?: return@forEach
                val result = when (info.key) {
                    BackupModule.COURSE.key -> restoreAllCourseTablesCbor(data)
                    BackupModule.STYLE.key -> restoreAppStyleBytes(data)
                    else -> Result.success(Unit)
                }
                if (result.isFailure) return@withContext result
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 1. 课表核心业务通道

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun exportAllCourseTablesCbor(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val allTablesFromDb = courseTableRepository.getAllCourseTables().first()
            if (allTablesFromDb.isEmpty()) return@withContext null
            val appSettings = appSettingsRepository.getAppSettingsOnce()

            val tablePacks = allTablesFromDb.mapNotNull { table ->
                val exportModel = courseConversionRepository.exportCourseTableToJson(table.id) ?: return@mapNotNull null
                SingleTablePack(table.id, table.name, table.createdAt, exportModel)
            }

            val envelope = TotalAppBackupEnvelope(
                backupTimestamp = System.currentTimeMillis(),
                appVersionCode = CourseImportExport.COURSE_SCHEMA_VERSION,
                currentCourseTableId = appSettings.currentCourseTableId,
                allTables = tablePacks
            )

            CourseImportExport.cbor.encodeToByteArray(TotalAppBackupEnvelope.serializer(), envelope)
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Transaction
    suspend fun restoreAllCourseTablesCbor(cborBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (cborBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException(context.getString(R.string.backup_err_empty)))
            }

            val envelope = try {
                CourseImportExport.cbor.decodeFromByteArray(TotalAppBackupEnvelope.serializer(), cborBytes)
            } catch (_: Exception) {
                return@withContext Result.failure(IllegalStateException(context.getString(R.string.backup_err_corrupted)))
            }

            if (envelope.appVersionCode > CourseImportExport.COURSE_SCHEMA_VERSION) {
                return@withContext Result.failure(IllegalStateException(context.getString(R.string.backup_err_version_too_new)))
            }

            courseTableRepository.getAllCourseTables().first().forEach { courseTableDao.delete(it) }

            envelope.allTables.forEach { pack ->
                courseTableDao.insert(CourseTable(pack.tableId, pack.tableName, pack.createdAt))
                val importModel = CourseTableImportModel(
                    courses = pack.tableData.courses.map {
                        ImportCourseJsonModel(it.id, it.name, it.teacher, it.position, it.day, it.startSection, it.endSection, it.weeks, it.isCustomTime, it.customStartTime, it.customEndTime, it.color, it.remark)
                    },
                    timeSlots = pack.tableData.timeSlots,
                    config = pack.tableData.config
                )
                courseConversionRepository.importCourseTableFromJson(pack.tableId, importModel)
            }

            val backupTargetTableId = envelope.currentCourseTableId
            val finalTableId = if (envelope.allTables.any { it.tableId == backupTargetTableId }) {
                backupTargetTableId
            } else {
                envelope.allTables.firstOrNull()?.tableId ?: ""
            }

            val currentSettings = appSettingsRepository.getAppSettingsOnce()
            appSettingsRepository.insertOrUpdateAppSettings(currentSettings.copy(currentCourseTableId = finalTableId))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. 个性化样式核心业务通道

    /**
     * 导出样式独立通道
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun exportAppStyleBytes(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val rawProtoBytes = styleSettingsRepository.exportRawStyleBytes()
            val envelope = StyleBackupEnvelope(
                backupTimestamp = System.currentTimeMillis(),
                appVersionCode = StyleSettingsRepository.STYLE_SCHEMA_VERSION,
                styleProtoBytes = rawProtoBytes
            )
            CourseImportExport.cbor.encodeToByteArray(StyleBackupEnvelope.serializer(), envelope)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 样式恢复独立通道
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun restoreAppStyleBytes(styleBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (styleBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException(context.getString(R.string.backup_err_empty)))
            }

            val envelope = try {
                CourseImportExport.cbor.decodeFromByteArray(StyleBackupEnvelope.serializer(), styleBytes)
            } catch (_: Exception) {
                return@withContext Result.failure(IllegalStateException(context.getString(R.string.backup_err_corrupted)))
            }

            if (envelope.appVersionCode > StyleSettingsRepository.STYLE_SCHEMA_VERSION) {
                return@withContext Result.failure(IllegalStateException(context.getString(R.string.backup_err_version_too_new)))
            }

            val migratedProtoBytes = if (envelope.appVersionCode < StyleSettingsRepository.STYLE_SCHEMA_VERSION) {
                envelope.styleProtoBytes
            } else {
                envelope.styleProtoBytes
            }
            styleSettingsRepository.restoreRawStyleBytes(migratedProtoBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}