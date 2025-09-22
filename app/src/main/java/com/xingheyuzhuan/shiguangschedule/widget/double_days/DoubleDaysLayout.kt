// DoubleDaysLayout.kt
package com.xingheyuzhuan.shiguangschedule.widget.double_days

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.widget.compact.VacationLayout
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as LocalDateTextStyle
import java.util.Locale

@Composable
fun DoubleDaysLayout(
    coursesAndWeekFlow: Flow<Pair<List<List<WidgetCourse>>, Int?>>
) {
    val coursesAndWeekState = coursesAndWeekFlow.collectAsState(
        initial = Pair(emptyList(), null)
    )
    val (coursesList, currentWeek) = coursesAndWeekState.value

    val todayCourses = coursesList.getOrElse(0) { emptyList() }
    val tomorrowCourses = coursesList.getOrElse(1) { emptyList() }

    val today = LocalDate.now()

    val todayDateString = today.format(DateTimeFormatter.ofPattern("M.d", Locale.getDefault()))
    val todayDayOfWeekString = today.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())

    val isVacation = currentWeek == null

    val systemCornerRadius = 21.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(systemCornerRadius)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // 顶部日历栏：日期、星期和周数 (与2x2小组件保持一致)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(GlanceTheme.colors.primary)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$todayDateString $todayDayOfWeekString",
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onPrimary)
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = if (currentWeek != null) "第${currentWeek}周" else "",
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onPrimary)
                )
            }

            // 主要内容区域：左右分割
            if (isVacation) {
                VacationLayout()
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // 左半边：今天
                    DayColumn(
                        title = "今天",
                        courses = todayCourses,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        isToday = true
                    )
                    // 垂直分割线
                    Box(
                        modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(GlanceTheme.colors.onSurfaceVariant)
                    ) {
                        Spacer(modifier = GlanceModifier.fillMaxSize())
                    }
                    // 右半边：明天
                    DayColumn(
                        title = "明天",
                        courses = tomorrowCourses,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        isToday = false
                    )
                }
            }
        }
    }
}

@Composable
fun DayColumn(title: String, courses: List<WidgetCourse>, modifier: GlanceModifier, isToday: Boolean) {
    val now = LocalTime.now()
    val nextCourse = if (isToday) {
        courses.filter { !it.isSkipped && LocalTime.parse(it.endTime) > now }.firstOrNull()
    } else {
        courses.firstOrNull { !it.isSkipped }
    }

    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        // 新的逻辑：将标题作为课程内容的一部分
        if (nextCourse != null) {
            CourseItem(course = nextCourse)
        } else if (courses.isNotEmpty() && isToday) {
            TodayCoursesFinishedLayout()
        } else if (courses.isEmpty() && isToday) {
            NoCoursesLayout()
        } else if (courses.isEmpty() && !isToday) {
            TomorrowNoCoursesLayout()
        } else {
            TomorrowNoCoursesLayout()
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        // 底部剩余课程数提示
        val remainingCoursesCount = courses.count {
            if (isToday) !it.isSkipped && LocalTime.parse(it.endTime) > now
            else !it.isSkipped
        }

        if (remainingCoursesCount > 0) {
            Text(
                text = "$title 还有 $remainingCoursesCount 节课",
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        } else {
            Spacer(modifier = GlanceModifier.height(12.dp))
        }
    }
}

@Composable
fun NoCoursesLayout() {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "今天没有课程",
            style = TextStyle(color = GlanceTheme.colors.onSurface)
        )
    }
}

@Composable
fun TomorrowNoCoursesLayout() {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "明天没有课程",
            style = TextStyle(color = GlanceTheme.colors.onSurface)
        )
    }
}

@Composable
fun TodayCoursesFinishedLayout() {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "今日课程已结束",
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
        )
    }
}

@Composable
fun CourseItem(course: WidgetCourse) {
    Column(
        horizontalAlignment = Alignment.Horizontal.Start,
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        // 课程名称
        Text(
            text = course.name,
            style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        // 课程时间
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.outline_calendar_clock_24),
                contentDescription = "时间图标",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.height(12.dp)
            )
            Text(
                text = "${course.startTime}-${course.endTime}",
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        }
        // 教室
        if (course.position.isNotBlank()) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.outline_location_on_24),
                    contentDescription = "位置图标",
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.height(12.dp)
                )
                Text(
                    text = course.position,
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1
                )
            }
        }
        // 老师
        if (course.teacher.isNotBlank()) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.outline_person_book_24),
                    contentDescription = "老师图标",
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.height(12.dp)
                )
                Text(
                    text = course.teacher,
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1
                )
            }
        }
    }
}