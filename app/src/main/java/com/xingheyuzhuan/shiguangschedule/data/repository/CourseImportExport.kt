package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlin.random.Random

object CourseImportExport {

    // 用于 JSON 导入和导出的配置模型（不含 showWeekends）
    @Serializable
    data class CourseConfigJsonModel(
        val semesterStartDate: String?,
        val semesterTotalWeeks: Int,
        val defaultClassDuration: Int,
        val defaultBreakDuration: Int,
        val firstDayOfWeek: Int
    )

    // 导入时使用的 JSON 模型
    @Serializable
    data class CourseTableImportModel(
        val courses: List<ImportCourseJsonModel>,
        val timeSlots: List<TimeSlotJsonModel>,
        val config: CourseConfigJsonModel? = null
    )

    @Serializable
    data class ImportCourseJsonModel(
        val id: String? = null,
        val name: String,
        val teacher: String,
        val position: String,
        val day: Int,
        val startSection: Int,
        val endSection: Int,
        val weeks: List<Int>,
        val color: Int? = null
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
        val startSection: Int,
        val endSection: Int,
        val color: Int, // 导出时颜色必须
        val weeks: List<Int>
    )

    // 导入和导出都通用的时间段模型
    @Serializable
    data class TimeSlotJsonModel(
        val number: Int,
        val startTime: String,
        val endTime: String
    )

    // 全局的颜色选择器
    fun getRandomColor(): Color {
        val colors = listOf(
            Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
            Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
            Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722)
        )
        return colors[Random.nextInt(colors.size)]
    }
}