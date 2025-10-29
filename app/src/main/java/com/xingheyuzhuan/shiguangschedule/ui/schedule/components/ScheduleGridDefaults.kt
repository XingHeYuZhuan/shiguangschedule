package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 定义课表网格和课程块的默认样式和尺寸。
 *
 * 这使得所有样式参数都集中在一个地方，便于管理和修改。
 */
object ScheduleGridDefaults {
    // 默认的左侧时间列表宽度
    val TimeColumnWidth: Dp @Composable get() = 40.dp

    // 默认的顶部星期栏高度
    val DayHeaderHeight: Dp @Composable get() = 45.dp

    // 默认的单个时间段（一节课）的高度
    val SectionHeight: Dp @Composable get() = 70.dp

    // --- CourseBlock 参数 ---

    // 课程块的圆角大小
    val CourseBlockCornerRadius: Dp = 4.dp

    // 课程块之间的外边距
    val CourseBlockOuterPadding: Dp = 1.dp

    // 课程块内部内容的内边距
    val CourseBlockInnerPadding: Dp = 4.dp

    // 课程块的透明度，值越小颜色越浅
    val CourseBlockAlpha: Float = 1f

    // 文本颜色变深因子，值越大颜色越深
    val TextDarkenFactor: Float = 0.618f

    // 冲突课程的颜色
    val ConflictCourseColor: Color = Color(0xFFFFB74D)

    // 非冲突课程的默认颜色
    val DefaultCourseColor: Color = Color(0xFF64B5F6)

    /**
     * 根据输入颜色返回一个更深的颜色。
     * @param color 原始颜色。
     * @param factor 变深因子，取值范围为 0.0 到 1.0，值越大颜色越深。
     */
    fun getDarkerColor(color: Color, factor: Float): Color {
        val red = (color.red * (1 - factor)).coerceIn(0f, 1f)
        val green = (color.green * (1 - factor)).coerceIn(0f, 1f)
        val blue = (color.blue * (1 - factor)).coerceIn(0f, 1f)
        return Color(red, green, blue, color.alpha)
    }
    /**
     * 判断一个颜色是亮色还是暗色。
     *
     * @return 如果颜色是亮色，返回 true；否则返回 false。
     */
    fun Color.isLight(): Boolean {
        // 亮度值在 0.5 以上通常被认为是亮色
        return this.luminance() > 0.5
    }
}