package com.xingheyuzhuan.shiguangschedule.widget.double_days

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import androidx.glance.layout.wrapContentWidth
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.widget.ScaledBitmapText
import com.xingheyuzhuan.shiguangschedule.widget.WidgetColors
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as LocalDateTextStyle


private const val BASE_WIDGET_WIDTH = 300f
private const val BASE_WIDGET_HEIGHT = 170f
private const val MAX_LAYOUT_SCALE = 4.0f

/**
 * 双日课程小组件的布局。
 */
@Composable
fun DoubleDaysLayout(coursesAndWeekFlow: Flow<Pair<List<List<WidgetCourse>>, Int?>>) {
    val context = LocalContext.current
    val currentSize = LocalSize.current

    val widthScale = currentSize.width.value / BASE_WIDGET_WIDTH
    val heightScale = currentSize.height.value / BASE_WIDGET_HEIGHT

    val rawScale = minOf(widthScale, heightScale)
    val finalScale = rawScale.coerceIn(1.0f, MAX_LAYOUT_SCALE)

    val coursesAndWeekState = coursesAndWeekFlow.collectAsState(initial = Pair(emptyList(), null))

    val (coursesList, currentWeek) = coursesAndWeekState.value

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val rawTodayCourses = coursesList.getOrNull(0) ?: emptyList()
    val rawTomorrowCourses = coursesList.getOrNull(1) ?: emptyList()

    val isVacation = currentWeek == null
    val now = LocalTime.now()
    val totalRemainingTodayCourses = rawTodayCourses.count { !it.isSkipped && LocalTime.parse(it.endTime) > now }
    val totalTomorrowCourses = rawTomorrowCourses.count { !it.isSkipped }

    val effectiveTodayCourses = rawTodayCourses.filter {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }.take(2)

    val effectiveTomorrowCourses = rawTomorrowCourses.filter { !it.isSkipped }.take(2)

    // 使用统一的 finalScale 来计算圆角
    val systemCornerRadius = (21 * finalScale).dp

    // 整个小组件的容器
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
            // 顶栏
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = (8 * finalScale).dp, vertical = (2 * finalScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.defaultWeight())

                ScaledBitmapText(
                    text = if (currentWeek != null) {
                        context.getString(R.string.status_current_week_format, currentWeek)
                    } else {
                        context.getString(R.string.title_vacation)
                    },
                    fontSizeDp = (13f * finalScale).dp,
                    color = WidgetColors.textHint,
                    modifier = GlanceModifier.wrapContentSize()
                )
            }

            if (isVacation) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = (20 * finalScale).dp, vertical = (20 * finalScale).dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    ScaledBitmapText(
                        text = context.getString(R.string.title_vacation),
                        fontSizeDp = (14f * finalScale).dp,
                        color = WidgetColors.textPrimary,
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                    ScaledBitmapText(
                        text = context.getString(R.string.widget_vacation_expecting),
                        fontSizeDp = (12f * finalScale).dp,
                        color = WidgetColors.textSecondary,
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(bottom = (6 * finalScale).dp),
                    verticalAlignment = Alignment.Top
                ) {
                    DayScheduleColumn(
                        date = today,
                        courses = effectiveTodayCourses,
                        modifier = GlanceModifier.defaultWeight(),
                        totalRemainingCount = totalRemainingTodayCourses,
                        scale = finalScale
                    )

                    // 中间分割线
                    Box(
                        modifier = GlanceModifier
                            .width((1 * finalScale).dp)
                            .fillMaxHeight()
                            .background(WidgetColors.divider)
                            .padding(vertical = (4 * finalScale).dp)
                    ) { /* content lambda 块 */ }

                    DayScheduleColumn(
                        date = tomorrow,
                        courses = effectiveTomorrowCourses,
                        modifier = GlanceModifier.defaultWeight(),
                        totalRemainingCount = totalTomorrowCourses,
                        scale = finalScale
                    )
                }
            }
        }
    }
}

/**
 * 单日课程列表列
 */
@Composable
fun DayScheduleColumn(
    date: LocalDate,
    courses: List<WidgetCourse>,
    modifier: GlanceModifier,
    totalRemainingCount: Int,
    scale: Float
) {
    val context = LocalContext.current
    val dateString = date.format(DateTimeFormatter.ofPattern("M.d", Locale.getDefault()))
    val dayOfWeekString = date.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())

    val dayTitleResId = if (date == LocalDate.now()) R.string.widget_title_today else R.string.widget_title_tomorrow

    Column(
        modifier = modifier.fillMaxHeight().padding(horizontal = (4 * scale).dp),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = (4 * scale).dp, vertical = (2 * scale).dp),
            verticalAlignment = Alignment.Top
        ) {
            ScaledBitmapText(
                text = context.getString(dayTitleResId),
                fontSizeDp = (13f * scale).dp,
                color = WidgetColors.textPrimary,
                modifier = GlanceModifier.wrapContentWidth().width((30 * scale).dp)
            )
            Spacer(modifier = GlanceModifier.width((4 * scale).dp))

            ScaledBitmapText(
                text = context.getString(
                    R.string.widget_date_dayofweek_format,
                    dateString,
                    dayOfWeekString
                ),
                fontSizeDp = (13f * scale).dp,
                color = WidgetColors.textHint,
                modifier = GlanceModifier.defaultWeight()
            )
        }

        Spacer(modifier = GlanceModifier.height(0.dp))

        if (totalRemainingCount == 0) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ScaledBitmapText(
                    text = context.getString(R.string.text_no_course),
                    fontSizeDp = (11f * scale).dp,
                    color = WidgetColors.textSecondary,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight()
            ) {
                val slots = 2
                (0 until slots).forEach { slotIndex ->
                    val course = courses.getOrNull(slotIndex)

                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (course != null) {
                            CourseItemDoubleDayContent(course = course, index = slotIndex, scale = scale)
                        }
                    }

                    if (slotIndex == 0) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().height((1 * scale).dp)
                                .padding(horizontal = (7 * scale).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .background(WidgetColors.divider)
                                    .cornerRadius((0.5 * scale).dp)
                            ) { /* content lambda 块 */ }
                        }
                    }
                }
            }

            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Spacer(modifier = GlanceModifier.height((4 * scale).dp))

                val textContent = when {
                    courses.isEmpty() && totalRemainingCount > 0 && date == LocalDate.now() ->
                        // "今日课程已结束"
                        context.getString(R.string.widget_today_courses_finished)
                    totalRemainingCount > 0 -> {
                        // "今日还有 X 节课" 或 "明天有 X 节课"
                        val formatResId = if (date == LocalDate.now()) {
                            R.string.widget_remaining_courses_format_today
                        } else {
                            R.string.widget_remaining_courses_format_tomorrow
                        }
                        context.getString(formatResId, totalRemainingCount)
                    }
                    else ->
                        // "无课程"
                        context.getString(R.string.text_no_course)
                }

                ScaledBitmapText(
                    text = textContent,
                    fontSizeDp = (11f * scale).dp,
                    color = WidgetColors.textHint,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
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
 * 双日布局中的单个课程项内容
 */
@Composable
fun CourseItemDoubleDayContent(
    course: WidgetCourse,
    index: Int,
    scale: Float
) {

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = (4 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(getCourseIndicatorDrawable(index)),
            contentDescription = null,
            modifier = GlanceModifier
                .width((4 * scale).dp)
                .fillMaxHeight()
        )

        // 使用统一的缩放因子
        Spacer(modifier = GlanceModifier.width((6 * scale).dp))

        // 右侧课程内容
        Column(
            // Column 只包住内容，并占据剩余宽度
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 课程名称 - 使用 ScaledBitmapText
            ScaledBitmapText(
                text = course.name,
                fontSizeDp = (12f * scale).dp,
                color = WidgetColors.textPrimary,
                modifier = GlanceModifier.fillMaxWidth()
            )

            // 教师信息
            if (course.teacher.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height((1 * scale).dp))
                ScaledBitmapText(
                    text = course.teacher,
                    fontSizeDp = (10f * scale).dp,
                    color = WidgetColors.textSecondary,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScaledBitmapText(
                    text = "${course.startTime.take(5)}-${course.endTime.take(5)}",
                    fontSizeDp = (10f * scale).dp,
                    color = WidgetColors.textTertiary,
                    modifier = GlanceModifier.wrapContentSize()
                )
                if (course.position.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.width((4 * scale).dp))
                    ScaledBitmapText(
                        text = course.position,
                        fontSizeDp = (10f * scale).dp,
                        color = WidgetColors.textHint,
                        modifier = GlanceModifier.wrapContentSize()
                    )
                }
            }
        }
    }
}