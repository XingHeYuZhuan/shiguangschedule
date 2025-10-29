package com.xingheyuzhuan.shiguangschedule.widget.compact

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.widget.ScaledBitmapText
import com.xingheyuzhuan.shiguangschedule.widget.WidgetColors
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.time.format.TextStyle as LocalDateTextStyle

private const val BASE_WIDGET_WIDTH = 180f
private const val BASE_WIDGET_HEIGHT = 180f
private const val MAX_LAYOUT_SCALE = 4.0f

@Composable
fun CompactLayout(coursesAndWeekFlow: Flow<Pair<List<WidgetCourse>, Int?>>) {
    // 1. 计算缩放因子
    val currentSize = LocalSize.current
    val widthScale = currentSize.width.value / BASE_WIDGET_WIDTH
    val heightScale = currentSize.height.value / BASE_WIDGET_HEIGHT

    val rawScale = minOf(widthScale, heightScale)
    val finalScale = rawScale.coerceIn(1.0f, MAX_LAYOUT_SCALE)

    val coursesAndWeekState = coursesAndWeekFlow.collectAsState(initial = Pair(emptyList(), null))
    val (courses, currentWeek) = coursesAndWeekState.value
    val today = LocalDate.now()
    val todayDayOfWeekString = today.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())

    val isVacation = currentWeek == null
    val now = LocalTime.now()

    // 获取接下来的 2 节课
    val nextCourses = courses.filter {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }.take(2)

    // 剩余未上课程总数
    val remainingCoursesCount = courses.count {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }

    // 缩放圆角半径
    val systemCornerRadius = (21 * finalScale).dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.background)
            .cornerRadius(systemCornerRadius)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // 顶部区域
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = (8 * finalScale).dp, vertical = (6 * finalScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScaledBitmapText(
                    text = todayDayOfWeekString,
                    fontSizeDp = (12f * finalScale).dp,
                    color = WidgetColors.textHint,
                    modifier = GlanceModifier.wrapContentSize()
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                if (currentWeek != null) {
                    ScaledBitmapText(
                        text = "第${currentWeek}周",
                        fontSizeDp = (12f * finalScale).dp,
                        color = WidgetColors.textHint,
                        modifier = GlanceModifier.wrapContentSize()
                    )
                }
            }

            // 主要内容区域：根据不同的状态显示不同的布局
            if (isVacation) {
                VacationLayout(scale = finalScale)
            } else if (courses.isEmpty()) {
                NoCoursesLayout(scale = finalScale)
            } else {

                // 课程列表区域：固定 2 个槽位
                Column(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    horizontalAlignment = Alignment.Horizontal.Start,
                    verticalAlignment = Alignment.Vertical.Top
                ) {
                    val slots = 2
                    (0 until slots).forEach { slotIndex ->
                        val course = nextCourses.getOrNull(slotIndex)

                        Box(
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (course != null) {
                                CourseItemCompact(course = course, index = slotIndex, scale = finalScale)
                            } else if (slotIndex == 0 && nextCourses.isEmpty()) {
                                if (remainingCoursesCount == 0) {
                                    Column(
                                        modifier = GlanceModifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                                        verticalAlignment = Alignment.Vertical.CenterVertically
                                    ) {
                                        ScaledBitmapText(
                                            text = "今日课程已结束",
                                            fontSizeDp = (12f * finalScale).dp,
                                            color = WidgetColors.textPrimary,
                                            modifier = GlanceModifier.wrapContentSize()
                                        )
                                    }
                                }
                            }
                        }

                        if (slotIndex < slots - 1 && nextCourses.isNotEmpty()) {
                            Spacer(modifier = GlanceModifier.height((3 * finalScale).dp))
                            Row(
                                modifier = GlanceModifier.fillMaxWidth()
                                    .padding(horizontal = (8 * finalScale).dp)
                            ) {
                                Spacer(modifier = GlanceModifier.width((4 * finalScale).dp))
                                Box(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .height((1 * finalScale).dp)
                                        .background(WidgetColors.divider)
                                        .cornerRadius((0.5 * finalScale).dp),
                                    content = {}
                                )
                            }
                            Spacer(modifier = GlanceModifier.height((3 * finalScale).dp))
                        }
                    }
                }

                if (remainingCoursesCount > 0) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = (8 * finalScale).dp)
                            .padding(bottom = (6 * finalScale).dp),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        ScaledBitmapText(
                            text = "今天还有 $remainingCoursesCount 节课",
                            fontSizeDp = (10f * finalScale).dp,
                            color = WidgetColors.textHint,
                            modifier = GlanceModifier.wrapContentSize()
                        )
                    }
                } else {
                    Spacer(modifier = GlanceModifier.height((6 * finalScale).dp))
                }
            }
        }
    }
}

/**
 * 假期布局
 */
@Composable
fun VacationLayout(scale: Float) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding((20 * scale).dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        ScaledBitmapText(
            text = "假期中",
            fontSizeDp = (12f * scale).dp,
            color = WidgetColors.textPrimary,
            modifier = GlanceModifier.wrapContentSize()
        )
        ScaledBitmapText(
            text = "期待新学期",
            fontSizeDp = (10f * scale).dp,
            color = WidgetColors.textSecondary,
            modifier = GlanceModifier.wrapContentSize()
        )
    }
}

/**
 * 无课程布局
 */
@Composable
fun NoCoursesLayout(scale: Float) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        ScaledBitmapText(
            text = "今天没有课程",
            fontSizeDp = (12f * scale).dp,
            color = WidgetColors.textPrimary,
            modifier = GlanceModifier.wrapContentSize()
        )
    }
}

/**
 * 获取课程指示器 Drawable 资源
 */
fun getCourseIndicatorDrawable(index: Int): Int {
    return when (index % 5) {
        0 -> R.drawable.course_indicator_blue
        1 -> R.drawable.course_indicator_red
        2 -> R.drawable.course_indicator_purple
        3 -> R.drawable.course_indicator_green
        else -> R.drawable.course_indicator_orange
    }
}

/**
 * 单个课程项内容
 */
@Composable
fun CourseItemCompact(course: WidgetCourse, index: Int, scale: Float) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = (8 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(getCourseIndicatorDrawable(index)),
            contentDescription = null,
            modifier = GlanceModifier
                .width((4 * scale).dp)
                .height((52 * scale).dp)
        )

        Spacer(modifier = GlanceModifier.width((8 * scale).dp))

        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            ScaledBitmapText(
                text = course.name,
                fontSizeDp = (13f * scale).dp,
                color = WidgetColors.textPrimary,
                modifier = GlanceModifier.fillMaxWidth()
            )

            Spacer(modifier = GlanceModifier.height((2 * scale).dp))

            // 教师信息
            if (course.teacher.isNotBlank()) {
                ScaledBitmapText(
                    text = course.teacher,
                    fontSizeDp = (11f * scale).dp,
                    color = WidgetColors.textSecondary,
                    modifier = GlanceModifier.fillMaxWidth()
                )
                Spacer(modifier = GlanceModifier.height((2 * scale).dp))
            }

            // 时间和地点
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScaledBitmapText(
                    text = "${course.startTime.substring(0, 5)}-${course.endTime.substring(0, 5)}",
                    fontSizeDp = (10f * scale).dp,
                    color = WidgetColors.textTertiary,
                    modifier = GlanceModifier.wrapContentSize()
                )
                if (course.position.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    ScaledBitmapText(
                        text = course.position,
                        fontSizeDp = (10f * scale).dp,
                        color = WidgetColors.textTertiary,
                        modifier = GlanceModifier.wrapContentSize()
                    )
                }
            }
        }
    }
}