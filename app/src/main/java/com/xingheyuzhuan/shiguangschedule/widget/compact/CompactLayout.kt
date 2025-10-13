package com.xingheyuzhuan.shiguangschedule.widget.compact

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
fun CompactLayout(coursesAndWeekFlow: Flow<Pair<List<WidgetCourse>, Int?>>) {
    val coursesAndWeekState = coursesAndWeekFlow.collectAsState(initial = Pair(emptyList(), null))
    val (courses, currentWeek) = coursesAndWeekState.value
    val today = LocalDate.now()
    val todayDateString = today.format(DateTimeFormatter.ofPattern("M.d", Locale.getDefault()))
    val todayDayOfWeekString = today.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())

    val isVacation = currentWeek == null
    val now = LocalTime.now()

    val nextCourse = courses.firstOrNull {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }

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
                VacationLayout()
            } else if (courses.isEmpty()) {
                NoCoursesLayout()
            } else {
                if (nextCourse != null) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        // 课程名称
                        Text(
                            text = nextCourse.name,
                            style = TextStyle(fontSize = 18.sp, color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        // 时间
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.outline_calendar_clock_24),
                                contentDescription = "时间图标",
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                                modifier = GlanceModifier.height(12.dp)
                            )
                            val timeText = "${nextCourse.startTime}-${nextCourse.endTime}"
                            Text(
                                text = timeText,
                                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }

                        // 教室位置
                        if (nextCourse.position.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.outline_location_on_24),
                                    contentDescription = "位置图标",
                                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                                    modifier = GlanceModifier.height(12.dp)
                                )
                                Text(
                                    text = nextCourse.position,
                                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant)
                                )
                            }
                        }

                        // 老师信息
                        if (nextCourse.teacher.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.outline_person_book_24),
                                    contentDescription = "老师图标",
                                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                                    modifier = GlanceModifier.height(12.dp)
                                )
                                Text(
                                    text = nextCourse.teacher,
                                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant)
                                )
                            }
                        }
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
                            style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // 这个 Spacer 占据剩余空间，将下面的 Row 推到最下方
                Spacer(modifier = GlanceModifier.defaultWeight())

                // 底部区域：剩余课程数
                if (nextCourse != null && remainingCoursesCount > 0) {
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
                } else {
                    Spacer(modifier = GlanceModifier.height(12.dp))
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
            style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold)
        )
        Text(
            text = "期待新学期",
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
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
            style = TextStyle(color = GlanceTheme.colors.onSurface)
        )
    }
}
