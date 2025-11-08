package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlin.random.Random

data class DualColor(val light: Color, val dark: Color)

object CourseImportExport {

    /**
     * 颜色映射表：索引（对应 Course.colorInt） -> 浅色模式颜色 & 深色模式颜色
     * 数据库中存储的是这个 List 的 Index (0, 1, 2...)
     */
    val COURSE_COLOR_MAPS = listOf(
        DualColor(light = Color(0xFFFFCC99), dark = Color(0xFF663300)),
        DualColor(light = Color(0xFFFFE699), dark = Color(0xFF664D00)),
        DualColor(light = Color(0xFFE6FF99), dark = Color(0xFF4D6600)),
        DualColor(light = Color(0xFFCCFF99), dark = Color(0xFF336600)),
        DualColor(light = Color(0xFF99FFB3), dark = Color(0xFF00661A)),
        DualColor(light = Color(0xFF99FFE6), dark = Color(0xFF00664D)),
        DualColor(light = Color(0xFF99FFFF), dark = Color(0xFF006666)),
        DualColor(light = Color(0xFF99E6FF), dark = Color(0xFF004D66)),
        DualColor(light = Color(0xFFB399FF), dark = Color(0xFF1A0066)),
        DualColor(light = Color(0xFFFF99E6), dark = Color(0xFF66004D)),
        DualColor(light = Color(0xFFFF99CC), dark = Color(0xFF660033)),
        DualColor(light = Color(0xFFFF99B3), dark = Color(0xFF66001A)),
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

    /**
     * 在创建新课程时使用，用于获取一个随机的颜色索引。
     */
    fun getRandomColorIndex(): Int {
        return Random.nextInt(COURSE_COLOR_MAPS.size)
    }
}