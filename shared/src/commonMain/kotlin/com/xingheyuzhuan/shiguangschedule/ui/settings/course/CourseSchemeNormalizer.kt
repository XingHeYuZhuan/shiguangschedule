package com.xingheyuzhuan.shiguangschedule.ui.settings.course

/**
 * 将同一课程名下完全相同、只有周次不同的方案合并为一条。
 * 合并键包含名称、星期、时间、老师、地点与备注，避免丢失差异信息。
 */
fun normalizeCourseSchemes(
    name: String,
    schemes: List<CourseScheme>
): List<CourseScheme> {
    val merged = LinkedHashMap<String, CourseScheme>()

    schemes.forEach { scheme ->
        val key = listOf(
            name,
            scheme.day.toString(),
            scheme.isCustomTime.toString(),
            if (scheme.isCustomTime) scheme.customStartTime else scheme.startSection.toString(),
            if (scheme.isCustomTime) scheme.customEndTime else scheme.endSection.toString(),
            scheme.teacher,
            scheme.position,
            scheme.remark
        ).joinToString("|")

        val existing = merged[key]
        if (existing == null) {
            merged[key] = scheme
        } else {
            merged[key] = existing.copy(weeks = existing.weeks + scheme.weeks)
        }
    }

    return merged.values.toList()
}
