package com.xingheyuzhuan.shiguangschedule.data.repository

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.cbor.Cbor


object CourseImportExport {

    /**
     * 核心数据规范版本号
     * 确立基础全量多课表 CBOR 备份协议规范
     * 未来如果重构了底层数据架构（如颠覆了基础字段或关联关系），可手动升级为 2，借此编写迁移清洗流
     */
    const val COURSE_SCHEMA_VERSION = 1

    /**
     * 自定义 Json 解析器
     * ignoreUnknownKeys = true: 确保旧版 App 遇到新加的字段（如 remark）时能跳过而不崩溃
     * encodeDefaults = true: 导出时即使字段是默认值也会包含在 JSON 中
     */
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    /**
     * CBOR 解析器（用于全盘多表备份、WebDAV 高密二进制流场景）
     */
    @OptIn(ExperimentalSerializationApi::class)
    val cbor = Cbor {
        ignoreUnknownKeys = true
    }

    /**
     * 全盘备份文件的最外层“集装箱”
     */
    @Serializable
    data class TotalAppBackupEnvelope(
        val backupTimestamp: Long,          // 备份生成的时间戳
        val appVersionCode: Int,            // 实际承载 CURRENT_SCHEMA_VERSION，代表数据协议版本
        val currentCourseTableId: String,   // 备份前用户当前激活/选中的课表 ID
        val allTables: List<SingleTablePack> // 系统中所有课表的总集合列表
    )

    /**
     * 单个课表资产的隔离包裹
     */
    @Serializable
    data class SingleTablePack(
        val tableId: String,          // 课表在数据库中的物理 UUID 主键
        val tableName: String,        // 课表名称
        val createdAt: Long,          // 课表本身的创建时间戳，用于恢复后列表排序
        val tableData: CourseTableExportModel // 直接复用单表导出模型
    )



    // 用于 JSON 导入和导出的配置模型
    @Serializable
    data class CourseConfigJsonModel(
        val semesterStartDate: String? = null,
        val semesterTotalWeeks: Int = 20,
        val defaultClassDuration: Int = 45,
        val defaultBreakDuration: Int = 10,
        val firstDayOfWeek: Int = 1
    )

    // 导入时使用的 JSON 模型
    @Serializable
    data class CourseTableImportModel(
        val courses: List<ImportCourseJsonModel>,
        val timeSlots: List<TimeSlotJsonModel>? = emptyList(),
        val config: CourseConfigJsonModel? = null
    )

    @Serializable
    data class ImportCourseJsonModel(
        val id: String? = null,
        val name: String,
        val teacher: String,
        val position: String,
        val day: Int,
        val startSection: Int? = null,
        val endSection: Int? = null,
        val weeks: List<Int>,
        val isCustomTime: Boolean = false,
        val customStartTime: String? = null,
        val customEndTime: String? = null,
        val color: Int? = null,
        val remark: String? = null
    )

    // 导出时使用的 JSON 模型
    @Serializable
    data class CourseTableExportModel(
        val courses: List<ExportCourseJsonModel>,
        val timeSlots: List<TimeSlotJsonModel>,
        val config: CourseConfigJsonModel
    )

    @Serializable
    data class ExportCourseJsonModel(
        val id: String, // 导出时id必须
        val name: String,
        val teacher: String,
        val position: String,
        val day: Int,
        val startSection: Int? = null,
        val endSection: Int? = null,
        val color: Int, // 导出时颜色必须
        val weeks: List<Int>,
        val isCustomTime: Boolean = false,
        val customStartTime: String? = null,
        val customEndTime: String? = null,
        val remark: String?
    )

    // 导入和导出都通用的时间段模型
    @Serializable
    data class TimeSlotJsonModel(
        val number: Int,
        val startTime: String,
        val endTime: String,
        val alias: String? = null
    )

}