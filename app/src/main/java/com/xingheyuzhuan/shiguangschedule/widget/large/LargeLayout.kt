// LargeLayout.kt
package com.xingheyuzhuan.shiguangschedule.widget.large

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
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as LocalDateTextStyle
import java.util.Locale

@Composable
fun LargeLayout(coursesAndWeekFlow: Flow<Pair<List<WidgetCourse>, Int?>>) {
    val coursesAndWeekState = coursesAndWeekFlow.collectAsState(initial = Pair(emptyList(), null))
    val (courses, currentWeek) = coursesAndWeekState.value
    val today = LocalDate.now()
    val todayDateString = today.format(DateTimeFormatter.ofPattern("M.d", Locale.getDefault()))
    val todayDayOfWeekString = today.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())

    val isVacation = currentWeek == null
    val now = LocalTime.now()

    // 重点修改：显示接下来4节课
    val nextCourses = courses.filter {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }.take(4)

    val remainingCoursesCount = courses.count {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }

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
            // 顶部日历栏：日期、星期和周数
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

            // 主要内容区域：根据不同的状态显示不同的布局
            if (isVacation) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "假期中",
                        style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "期待新学期",
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            } else if (courses.isEmpty()) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = "今天没有课程",
                        style = TextStyle(color = GlanceTheme.colors.onSurface)
                    )
                }
            } else {
                if (nextCourses.isNotEmpty()) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.Horizontal.Start,
                        verticalAlignment = Alignment.Vertical.Top
                    ) {
                        nextCourses.forEachIndexed { index, course ->
                            CourseItem(course = course)
                            if (index < nextCourses.size - 1) {
                                Spacer(modifier = GlanceModifier.height(8.dp))
                            }
                        }
                        // 底部区域：剩余课程数
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        if (remainingCoursesCount > 0) {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 12.dp),
                                horizontalAlignment = Alignment.Horizontal.Start
                            ) {
                                Text(
                                    text = "今天还有 $remainingCoursesCount 节课",
                                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                                )
                            }
                        }
                    }
                } else {
                    // 今日课程已全部结束
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "今日课程已结束",
                            style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

// 保持 CourseItem 函数不变
@Composable
fun CourseItem(course: WidgetCourse) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧时间信息 (垂直排列)
        Column(
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            val startTime = LocalTime.parse(course.startTime).format(DateTimeFormatter.ofPattern("HH:mm"))
            val endTime = LocalTime.parse(course.endTime).format(DateTimeFormatter.ofPattern("HH:mm"))
            Text(
                text = startTime,
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = endTime,
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        }

        Spacer(modifier = GlanceModifier.width(16.dp))

        // 右侧带卡片的课程信息
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(12.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                // 课程名称
                Text(
                    text = course.name,
                    style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onSecondaryContainer, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                // 教室位置和老师信息
                if (course.position.isNotBlank() || course.teacher.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.outline_location_on_24),
                            contentDescription = "位置图标",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                            modifier = GlanceModifier.height(12.dp)
                        )
                        Text(
                            text = course.position,
                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSecondaryContainer),
                            maxLines = 1
                        )
                        if (course.teacher.isNotBlank()) {
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Image(
                                provider = ImageProvider(R.drawable.outline_person_book_24),
                                contentDescription = "老师图标",
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                                modifier = GlanceModifier.height(12.dp)
                            )
                            Text(
                                text = course.teacher,
                                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSecondaryContainer),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}