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
fun CompactLayout(multiDayCoursesAndWeekFlow: Flow<Pair<List<List<WidgetCourse>>, Int?>>) {
    // 1. 计算缩放因子
    val currentSize = LocalSize.current
    val widthScale = currentSize.width.value / BASE_WIDGET_WIDTH
    val heightScale = currentSize.height.value / BASE_WIDGET_HEIGHT

    val rawScale = minOf(widthScale, heightScale)
    val finalScale = rawScale.coerceIn(1.0f, MAX_LAYOUT_SCALE)
    val coursesAndWeekState = multiDayCoursesAndWeekFlow.collectAsState(initial = Pair(emptyList(), null))
    val (allCoursesLists, currentWeek) = coursesAndWeekState.value

    val todayCourses = allCoursesLists.firstOrNull() ?: emptyList()
    val tomorrowCourses = allCoursesLists.getOrNull(1) ?: emptyList()

    val isVacation = currentWeek == null
    val now = LocalTime.now()

    // 今天剩余未上课程总数
    val remainingTodayCoursesCount = todayCourses.count {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }

    val isTodayFinished = remainingTodayCoursesCount == 0
    val hasTomorrowCourses = tomorrowCourses.isNotEmpty()

    val isShowingTomorrow = !isVacation && (todayCourses.isEmpty() || isTodayFinished) && hasTomorrowCourses

    val displayCourses = if (isShowingTomorrow) tomorrowCourses else todayCourses
    val displayRemainingCount = if (isShowingTomorrow) tomorrowCourses.count { !it.isSkipped } else remainingTodayCoursesCount

    val displayDate = if (isShowingTomorrow) LocalDate.now().plusDays(1) else LocalDate.now()

    // 确定顶部的文本
    val topText = if (isShowingTomorrow) {
        "明日课程预告"
    } else {
        displayDate.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())
    }
    val subTopText = if (isShowingTomorrow) {
        displayDate.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())
    } else {
        ""
    }

    // 获取接下来的 2 节课
    val nextCourses = if (isShowingTomorrow) {
        displayCourses.filter { !it.isSkipped }.take(2)
    } else {
        displayCourses.filter {
            !it.isSkipped && LocalTime.parse(it.endTime) > now
        }.take(2)
    }

    val shouldShowCenterStatusText = !isVacation && (
            displayCourses.isEmpty()
                    || (!isShowingTomorrow && nextCourses.isEmpty())
            )


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
            // 顶部区域 (保持不变)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = (8 * finalScale).dp, vertical = (6 * finalScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScaledBitmapText(
                        text = topText,
                        fontSizeDp = (12f * finalScale).dp,
                        color = WidgetColors.textHint,
                        modifier = GlanceModifier.wrapContentSize()
                    )
                    if (subTopText.isNotBlank()) {
                        Spacer(modifier = GlanceModifier.width((4 * finalScale).dp))
                        ScaledBitmapText(
                            text = subTopText,
                            fontSizeDp = (12f * finalScale).dp,
                            color = WidgetColors.textHint,
                            modifier = GlanceModifier.wrapContentSize()
                        )
                    }
                }

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

            if (isVacation) {
                VacationLayout(scale = finalScale)
            } else if (shouldShowCenterStatusText) {
                val statusText = if (displayCourses.isEmpty()) {
                    if (isShowingTomorrow) "明天没有课程" else "今天没有课程"
                } else {
                    "今日课程已结束"
                }
                NoCoursesLayout(scale = finalScale, statusText = statusText)
            } else {
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
                            }
                        }

                        if (slotIndex < slots - 1 && nextCourses.size > slotIndex + 1) {
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

                // 底部剩余课程数提示
                if (displayRemainingCount > 0) {
                    val baseText = if (isShowingTomorrow) "明天" else "今日"
                    val actionText = if (isShowingTomorrow) "" else "还"
                    val totalText = "节课"

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = (8 * finalScale).dp)
                            .padding(bottom = (6 * finalScale).dp),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        ScaledBitmapText(
                            text = "$baseText${actionText}有${displayRemainingCount}${totalText}",
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
 * 无课程布局
 */
@Composable
fun NoCoursesLayout(scale: Float, statusText: String) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        ScaledBitmapText(
            text = statusText,
            fontSizeDp = (12f * scale).dp,
            color = WidgetColors.textPrimary,
            modifier = GlanceModifier.wrapContentSize()
        )
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