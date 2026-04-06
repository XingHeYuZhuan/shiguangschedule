package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto

// 1. Compose 业务模型

/**
 * 浅色和深色模式下的颜色对。
 */
data class DualColor(val light: Color, val dark: Color)

/**
 * 课表网格样式配置的业务模型
 * 所有尺寸（Dp）属性使用 Float，颜色（Color）属性使用 Long。
 */
data class ScheduleGridStyle(
    // Grid 尺寸 (单位: Float/Dp)
    val timeColumnWidthDp: Float = DEFAULT_TIME_COLUMN_WIDTH,
    val dayHeaderHeightDp: Float = DEFAULT_DAY_HEADER_HEIGHT,
    val sectionHeightDp: Float = DEFAULT_SECTION_HEIGHT,

    // CourseBlock 外观 (单位: Float/Dp & Float)
    val courseBlockCornerRadiusDp: Float = DEFAULT_BLOCK_CORNER_RADIUS,
    val courseBlockOuterPaddingDp: Float = DEFAULT_BLOCK_OUTER_PADDING,
    val courseBlockInnerPaddingDp: Float = DEFAULT_BLOCK_INNER_PADDING,
    val courseBlockAlphaFloat: Float = DEFAULT_BLOCK_ALPHA,

    // 颜色列表
    val courseColorMaps: List<DualColor> = DEFAULT_COLOR_MAPS,

    val courseBlockFontScale: Float = DEFAULT_FONT_SCALE,

    // 界面开关与布局控制
    val hideGridLines: Boolean = false,
    val hideSectionTime: Boolean = false,
    val hideDateUnderDay: Boolean = false,
    val showStartTime: Boolean = false,
    val hideLocation: Boolean = false,
    val hideTeacher: Boolean = false,
    val removeLocationAt: Boolean = false,
    val textAlignCenterHorizontal: Boolean = false,
    val textAlignCenterVertical: Boolean = false,
    val borderType: BorderTypeProto = BorderTypeProto.BORDER_TYPE_NONE,
    val overlapStyleToggle: Boolean = false,

    // 背景壁纸路径 (存储在私有目录下的绝对路径)
    val backgroundImagePath: String? = null
) {

    fun generateRandomColorIndex(): Int {
        if (courseColorMaps.isEmpty()) return 0
        return kotlin.random.Random.nextInt(courseColorMaps.size)
    }

    companion object {
        // --- 默认常量 ---
        internal val DEFAULT_TIME_COLUMN_WIDTH = 40f
        internal val DEFAULT_DAY_HEADER_HEIGHT = 45f
        internal val DEFAULT_SECTION_HEIGHT = 70f
        internal val DEFAULT_BLOCK_CORNER_RADIUS = 4f
        internal val DEFAULT_BLOCK_OUTER_PADDING = 1f
        internal val DEFAULT_BLOCK_INNER_PADDING = 4f
        internal val DEFAULT_BLOCK_ALPHA = 1f
        internal val DEFAULT_FONT_SCALE = 1f

        internal val DEFAULT_COLOR_MAPS = listOf(
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

        /**
         * 默认样式对象，用于首次启动或重置样式。
         */
        val DEFAULT = ScheduleGridStyle(
            timeColumnWidthDp = DEFAULT_TIME_COLUMN_WIDTH,
            dayHeaderHeightDp = DEFAULT_DAY_HEADER_HEIGHT,
            sectionHeightDp = DEFAULT_SECTION_HEIGHT,
            courseBlockCornerRadiusDp = DEFAULT_BLOCK_CORNER_RADIUS,
            courseBlockOuterPaddingDp = DEFAULT_BLOCK_OUTER_PADDING,
            courseBlockInnerPaddingDp = DEFAULT_BLOCK_INNER_PADDING,
            courseBlockAlphaFloat = DEFAULT_BLOCK_ALPHA,
            courseColorMaps = DEFAULT_COLOR_MAPS,
            courseBlockFontScale = DEFAULT_FONT_SCALE,
            hideGridLines = false,
            hideSectionTime = false,
            hideDateUnderDay = false,
            showStartTime = false,
            hideLocation = false,
            hideTeacher = false,
            removeLocationAt = false,
            textAlignCenterHorizontal = false,
            textAlignCenterVertical = false,
            borderType = BorderTypeProto.BORDER_TYPE_NONE,
            overlapStyleToggle = false,
            backgroundImagePath = null
        )
    }
}


// 2. Proto ⇔ Compose 转换扩展函数

fun DualColorProto.toCompose(): DualColor {
    return DualColor(
        light = Color(this.lightColor.toInt()),
        dark = Color(this.darkColor.toInt())
    )
}

fun DualColor.toProto(): DualColorProto {
    return DualColorProto.newBuilder()
        .setLightColor(this.light.toArgb().toLong())
        .setDarkColor(this.dark.toArgb().toLong())
        .build()
}

/**
 * Protobuf -> ScheduleGridStyle 转换 function
 */
fun ScheduleGridStyleProto.toCompose(): ScheduleGridStyle {
    val d = ScheduleGridStyle.DEFAULT

    return ScheduleGridStyle(
        // 1. 基础布局尺寸
        timeColumnWidthDp = if (hasTimeColumnWidthDp()) timeColumnWidthDp else d.timeColumnWidthDp,
        dayHeaderHeightDp = if (hasDayHeaderHeightDp()) dayHeaderHeightDp else d.dayHeaderHeightDp,
        sectionHeightDp = if (hasSectionHeightDp()) sectionHeightDp else d.sectionHeightDp,

        // 2. 课程块外观
        courseBlockCornerRadiusDp = if (hasCourseBlockCornerRadiusDp()) courseBlockCornerRadiusDp else d.courseBlockCornerRadiusDp,
        courseBlockOuterPaddingDp = if (hasCourseBlockOuterPaddingDp()) courseBlockOuterPaddingDp else d.courseBlockOuterPaddingDp,
        courseBlockInnerPaddingDp = if (hasCourseBlockInnerPaddingDp()) courseBlockInnerPaddingDp else d.courseBlockInnerPaddingDp,

        // 3. 透明度与缩放
        courseBlockAlphaFloat = if (hasCourseBlockAlphaFloat()) courseBlockAlphaFloat else d.courseBlockAlphaFloat,
        courseBlockFontScale = if (hasCourseBlockFontScale()) courseBlockFontScale else d.courseBlockFontScale,

        // 5. 列表转换
        courseColorMaps = if (this.courseColorMapsList.isEmpty()) d.courseColorMaps else this.courseColorMapsList.map { it.toCompose() },

        // 6. 开关映射
        hideGridLines = if (hasHideGridLines()) hideGridLines else d.hideGridLines,
        hideSectionTime = if (hasHideSectionTime()) hideSectionTime else d.hideSectionTime,
        hideDateUnderDay = if (hasHideDateUnderDay()) hideDateUnderDay else d.hideDateUnderDay,
        showStartTime = if (hasShowStartTime()) showStartTime else d.showStartTime,
        hideLocation = if (hasHideLocation()) hideLocation else d.hideLocation,
        hideTeacher = if (hasHideTeacher()) hideTeacher else d.hideTeacher,
        removeLocationAt = if (hasRemoveLocationAt()) removeLocationAt else d.removeLocationAt,

        // 7. 对齐与边框
        textAlignCenterHorizontal = if (hasTextAlignCenterHorizontal()) textAlignCenterHorizontal else d.textAlignCenterHorizontal,
        textAlignCenterVertical = if (hasTextAlignCenterVertical()) textAlignCenterVertical else d.textAlignCenterVertical,
        borderType = if (hasBorderType()) borderType else d.borderType,
        overlapStyleToggle = if (hasOverlapStyleToggle()) overlapStyleToggle else d.overlapStyleToggle,

        // 8. 背景图路径映射
        backgroundImagePath = if (hasBackgroundImagePath() && backgroundImagePath.isNotEmpty()) backgroundImagePath else null
    )
}

/**
 * ScheduleGridStyle -> Protobuf 转换 (用于写入)
 */
fun ScheduleGridStyle.toProto(): ScheduleGridStyleProto {
    return ScheduleGridStyleProto.newBuilder().apply {
        timeColumnWidthDp = this@toProto.timeColumnWidthDp
        dayHeaderHeightDp = this@toProto.dayHeaderHeightDp
        sectionHeightDp = this@toProto.sectionHeightDp
        courseBlockCornerRadiusDp = this@toProto.courseBlockCornerRadiusDp
        courseBlockOuterPaddingDp = this@toProto.courseBlockOuterPaddingDp
        courseBlockInnerPaddingDp = this@toProto.courseBlockInnerPaddingDp
        courseBlockAlphaFloat = this@toProto.courseBlockAlphaFloat
        courseBlockFontScale = this@toProto.courseBlockFontScale

        addAllCourseColorMaps(this@toProto.courseColorMaps.map { it.toProto() })

        hideGridLines = this@toProto.hideGridLines
        hideSectionTime = this@toProto.hideSectionTime
        hideDateUnderDay = this@toProto.hideDateUnderDay
        showStartTime = this@toProto.showStartTime
        hideLocation = this@toProto.hideLocation
        hideTeacher = this@toProto.hideTeacher
        removeLocationAt = this@toProto.removeLocationAt
        textAlignCenterHorizontal = this@toProto.textAlignCenterHorizontal
        textAlignCenterVertical = this@toProto.textAlignCenterVertical
        borderType = this@toProto.borderType
        overlapStyleToggle = this@toProto.overlapStyleToggle

        // 路径映射
        backgroundImagePath = this@toProto.backgroundImagePath ?: ""
    }.build()
}