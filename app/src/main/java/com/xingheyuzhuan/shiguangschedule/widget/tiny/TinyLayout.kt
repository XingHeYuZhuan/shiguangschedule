package com.xingheyuzhuan.shiguangschedule.widget.tiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
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
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.widget.WidgetColors
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as LocalDateTextStyle
import java.util.Locale

@Composable
fun TinyLayout(coursesAndWeekFlow: Flow<Pair<List<WidgetCourse>, Int?>>) {
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
            // 顶部区域：星期X（左侧显示）和 周数（右侧显示）
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧
                Text(
                    text = if (currentWeek != null) todayDayOfWeekString else "$todayDateString $todayDayOfWeekString",
                    style = TextStyle(fontSize = 10.sp, color = WidgetColors.textHint)
                )

                Spacer(modifier = GlanceModifier.defaultWeight())
                // 右侧
                if (currentWeek != null) {
                    Text(
                        text = "第${currentWeek}周",
                        style = TextStyle(fontSize = 10.sp, color = WidgetColors.textHint)
                    )
                }
            }

            // 主要内容区域
            if (isVacation) {
                TinyVacationLayout()
            } else if (courses.isEmpty()) {
                TinyNoCoursesLayout()
            } else {
                if (nextCourse != null) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // 左侧彩色指示条
                        Box(
                            modifier = GlanceModifier
                                .width(2.dp)
                                .height(40.dp)
                                .background(WidgetColors.courseIndicator1),
                            content = {}
                        )

                        Spacer(modifier = GlanceModifier.width(6.dp))

                        // 右侧课程内容（紧凑显示）
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.Horizontal.Start
                        ) {
                            // 课程名称
                            Text(
                                text = nextCourse.name,
                                style = TextStyle(fontSize = 14.sp, color = WidgetColors.textPrimary, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )

                            // 时间和教师在同一行
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 时间段
                                Text(
                                    text = "${nextCourse.startTime.substring(0, 5)}-${nextCourse.endTime.substring(0, 5)}",
                                    style = TextStyle(fontSize = 11.sp, color = WidgetColors.textTertiary)
                                )

                                // 教师信息
                                if (nextCourse.teacher.isNotBlank()) {
                                    Text(
                                        text = " ",
                                        style = TextStyle(fontSize = 11.sp, color = WidgetColors.textSecondary)
                                    )
                                    Text(
                                        text = nextCourse.teacher,
                                        style = TextStyle(fontSize = 11.sp, color = WidgetColors.textSecondary),
                                        maxLines = 1
                                    )
                                }
                            }

                            // 地点在下一行
                            if (nextCourse.position.isNotBlank()) {
                                Text(
                                    text = nextCourse.position,
                                    style = TextStyle(fontSize = 11.sp, color = WidgetColors.textTertiary),
                                    maxLines = 1
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
                            text = "课程已结束",
                            style = TextStyle(fontSize = 12.sp, color = WidgetColors.textPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TinyVacationLayout() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "假期中",
            style = TextStyle(fontSize = 12.sp, color = WidgetColors.textPrimary, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun TinyNoCoursesLayout() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = "今天没课",
            style = TextStyle(fontSize = 12.sp, color = WidgetColors.textPrimary)
        )
    }
}