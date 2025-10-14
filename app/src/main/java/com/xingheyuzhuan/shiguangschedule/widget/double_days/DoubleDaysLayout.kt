package com.xingheyuzhuan.shiguangschedule.widget.double_days

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.width
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.widget.WidgetColors
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as LocalDateTextStyle
import java.util.Locale

/**
 * 双日课程小组件的布局。
 * 显示今天和明天的课程，每边最多显示2节，并在底部显示剩余课程总数。
 */
@Composable
fun DoubleDaysLayout(coursesAndWeekFlow: Flow<Pair<List<List<WidgetCourse>>, Int?>>) {
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
    val systemCornerRadius = 21.dp

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
            // 顶部区域：只显示周数或假期状态
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = if (currentWeek != null) "第${currentWeek}周" else "假期中",
                    style = TextStyle(fontSize = 12.sp, color = WidgetColors.textHint)
                )
            }

            // 主要内容区域：根据不同的状态显示不同的布局
            if (isVacation) {
                // 假期中 (全局一体化显示)
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "假期中",
                        style = TextStyle(fontSize = 14.sp, color = WidgetColors.textPrimary, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "期待新学期",
                        style = TextStyle(fontSize = 12.sp, color = WidgetColors.textSecondary)
                    )
                }
            } else {
                // 非假期状态下，总是显示左右两栏，由 DayScheduleColumn 负责各自的“无课程”状态
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 左侧：今天的课程
                    DayScheduleColumn(
                        date = today,
                        courses = effectiveTodayCourses,
                        modifier = GlanceModifier.defaultWeight(),
                        totalRemainingCount = totalRemainingTodayCourses
                    )

                    // 中间分割线
                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(WidgetColors.divider)
                            .padding(vertical = 4.dp)
                    ) { /* content lambda 块 */ }

                    // 右侧：明天的课程
                    DayScheduleColumn(
                        date = tomorrow,
                        courses = effectiveTomorrowCourses,
                        modifier = GlanceModifier.defaultWeight(),
                        totalRemainingCount = totalTomorrowCourses
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
    totalRemainingCount: Int
) {
    val dateString = date.format(DateTimeFormatter.ofPattern("M.d", Locale.getDefault()))
    val dayOfWeekString = date.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())
    val dayTitle = if (date == LocalDate.now()) "今天" else "明天"

    Column(
        modifier = modifier.fillMaxHeight().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        // 日期和标题
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayTitle,
                style = TextStyle(fontSize = 13.sp, color = WidgetColors.textPrimary, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = "$dateString $dayOfWeekString",
                style = TextStyle(fontSize = 11.sp, color = WidgetColors.textHint)
            )
        }

        Spacer(modifier = GlanceModifier.height(0.dp))

        if (totalRemainingCount == 0) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "无课程",
                    style = TextStyle(fontSize = 11.sp, color = WidgetColors.textSecondary)
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                courses.forEachIndexed { index, course ->
                    CourseItemDoubleDay(course = course, index = index)
                    if (index < courses.size - 1) {
                        Spacer(modifier = GlanceModifier.height(3.dp))
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 3.dp)
                        ) {
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .height(1.dp)
                                    .background(WidgetColors.divider)
                                    .cornerRadius(0.5.dp)
                            ) { /* content lambda 块 */ }
                        }
                        Spacer(modifier = GlanceModifier.height(3.dp))
                    }
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // 底部区域：显示剩余课程总数
            Column(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                // 显示剩余课程总数
                Text(
                    text = if (courses.isEmpty() && totalRemainingCount > 0 && date == LocalDate.now()) {
                        "今日课程已结束"
                    } else if (courses.isEmpty() && totalRemainingCount > 0) {
                        "$dayTitle 还有${totalRemainingCount}节课"
                    } else {
                        "$dayTitle 还有${totalRemainingCount}节课"
                    },
                    style = TextStyle(fontSize = 11.sp, color = WidgetColors.textHint)
                )
            }
        }
    }
}

/**
 * 获取课程指示器 Drawable 资源 (复用 ModerateLayout 的逻辑)
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
 * 双日布局中的单个课程项
 */
@Composable
fun CourseItemDoubleDay(course: WidgetCourse, index: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧彩色指示条
        Image(
            provider = ImageProvider(getCourseIndicatorDrawable(index)),
            contentDescription = null,
            modifier = GlanceModifier
                .width(4.dp)
                // 调整高度以适应更多信息行
                .height(42.dp)
        )

        Spacer(modifier = GlanceModifier.width(6.dp))

        // 右侧课程内容
        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            // 课程名称
            Text(
                text = course.name,
                style = TextStyle(fontSize = 12.sp, color = WidgetColors.textPrimary, fontWeight = FontWeight.Medium),
                maxLines = 1
            )

            // 教师信息
            if (course.teacher.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(1.dp)) // 课程名和教师之间的间距
                Text(
                    text = course.teacher,
                    style = TextStyle(fontSize = 10.sp, color = WidgetColors.textSecondary), // 使用次级颜色
                    maxLines = 1
                )
            }

            // 时间和地点
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${course.startTime.substring(0, 5)}-${course.endTime.substring(0, 5)}",
                    style = TextStyle(fontSize = 10.sp, color = WidgetColors.textTertiary),
                    maxLines = 1
                )
                if (course.position.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = course.position,
                        style = TextStyle(fontSize = 10.sp, color = WidgetColors.textHint),
                        maxLines = 1
                    )
                }
            }
        }
    }
}