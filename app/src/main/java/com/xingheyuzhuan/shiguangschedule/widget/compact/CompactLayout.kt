package com.xingheyuzhuan.shiguangschedule.widget.compact

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
import androidx.glance.layout.padding
import androidx.glance.layout.width
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

@Composable
fun CompactLayout(coursesAndWeekFlow: Flow<Pair<List<WidgetCourse>, Int?>>) {
    val coursesAndWeekState = coursesAndWeekFlow.collectAsState(initial = Pair(emptyList(), null))
    val (courses, currentWeek) = coursesAndWeekState.value
    val today = LocalDate.now()
    val todayDateString = today.format(DateTimeFormatter.ofPattern("M.d", Locale.getDefault()))
    val todayDayOfWeekString = today.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())

    val isVacation = currentWeek == null
    val now = LocalTime.now()

    val nextCourses = courses.filter {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }.take(2)

    val remainingCoursesCount = courses.count {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }

    val systemCornerRadius = 21.dp

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
            // 顶部区域：日期、星期和周数（右上角显示）
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = if (currentWeek != null) "第${currentWeek}周 $todayDateString $todayDayOfWeekString" else "$todayDateString $todayDayOfWeekString",
                    style = TextStyle(fontSize = 12.sp, color = WidgetColors.textHint)
                )
            }

            // 主要内容区域：根据不同的状态显示不同的布局
            if (isVacation) {
                VacationLayout()
            } else if (courses.isEmpty()) {
                NoCoursesLayout()
            } else {
                if (nextCourses.isNotEmpty()) {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.Horizontal.Start,
                        verticalAlignment = Alignment.Vertical.Top
                    ) {
                        nextCourses.forEachIndexed { index, course ->
                            CourseItemCompact(course = course, index = index)
                            if (index < nextCourses.size - 1) {
                                Spacer(modifier = GlanceModifier.height(3.dp))
                                // 分割线（从装饰线圆弧延伸，左侧对齐 8dp + 4dp，有圆角）
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp)
                                ) {
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                    Box(
                                        modifier = GlanceModifier
                                            .defaultWeight()
                                            .height(1.dp)
                                            .background(WidgetColors.divider)
                                            .cornerRadius(0.5.dp),
                                        content = {}
                                    )
                                }
                                Spacer(modifier = GlanceModifier.height(3.dp))
                            }
                        }
                        Spacer(modifier = GlanceModifier.defaultWeight())
                    }
                } else {
                    // 今日课程已全部结束
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "今日课程已结束",
                            style = TextStyle(fontSize = 12.sp, color = WidgetColors.textPrimary)
                        )
                    }
                }

                // 底部区域：剩余课程数
                if (remainingCoursesCount > 0) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        Text(
                            text = "今天还有 $remainingCoursesCount 节课",
                            style = TextStyle(fontSize = 10.sp, color = WidgetColors.textHint)
                        )
                    }
                } else {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun VacationLayout() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "假期中",
            style = TextStyle(fontSize = 12.sp, color = WidgetColors.textPrimary, fontWeight = FontWeight.Bold)
        )
        Text(
            text = "期待新学期",
            style = TextStyle(fontSize = 10.sp, color = WidgetColors.textSecondary)
        )
    }
}

@Composable
fun NoCoursesLayout() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = "今天没有课程",
            style = TextStyle(fontSize = 12.sp, color = WidgetColors.textPrimary)
        )
    }
}

// 获取课程指示器 Drawable 资源
fun getCourseIndicatorDrawable(index: Int): Int {
    return when (index % 5) {
        0 -> R.drawable.course_indicator_blue
        1 -> R.drawable.course_indicator_red
        2 -> R.drawable.course_indicator_purple
        3 -> R.drawable.course_indicator_green
        else -> R.drawable.course_indicator_orange
    }
}

@Composable
fun CourseItemCompact(course: WidgetCourse, index: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧彩色指示条（圆弧过渡，覆盖整个课程块）
        Image(
            provider = ImageProvider(getCourseIndicatorDrawable(index)),
            contentDescription = null,
            modifier = GlanceModifier
                .width(4.dp)
                .height(52.dp)
        )
        
        Spacer(modifier = GlanceModifier.width(8.dp))
        
        // 右侧课程内容
        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            // 课程名称
            Text(
                text = course.name,
                style = TextStyle(fontSize = 13.sp, color = WidgetColors.textPrimary, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            
            // 教师信息
            if (course.teacher.isNotBlank()) {
                Text(
                    text = course.teacher,
                    style = TextStyle(fontSize = 11.sp, color = WidgetColors.textSecondary),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
            
            // 时间和地点
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${course.startTime}-${course.endTime}",
                    style = TextStyle(fontSize = 10.sp, color = WidgetColors.textTertiary)
                )
                if (course.position.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = course.position,
                        style = TextStyle(fontSize = 10.sp, color = WidgetColors.textTertiary),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
