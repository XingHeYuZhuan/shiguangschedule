package com.xingheyuzhuan.shiguangschedule.widget.tiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
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

private const val BASE_WIDGET_WIDTH = 100f
private const val BASE_WIDGET_HEIGHT = 100f
private const val MAX_LAYOUT_SCALE = 3.0f

@Composable
fun TinyLayout(coursesAndWeekFlow: Flow<Pair<List<WidgetCourse>, Int?>>) {
    val context = LocalContext.current
    // 1. 计算缩放因子
    val currentSize = LocalSize.current
    val widthScale = currentSize.width.value / BASE_WIDGET_WIDTH
    val heightScale = currentSize.height.value / BASE_WIDGET_HEIGHT

    val rawScale = minOf(widthScale, heightScale)
    val finalScale = rawScale.coerceIn(1.0f, MAX_LAYOUT_SCALE)

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
            // 顶部区域：星期X（左侧显示）和 周数（右侧显示）
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    // 缩放 padding
                    .padding(horizontal = (6 * finalScale).dp, vertical = (4 * finalScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧
                ScaledBitmapText(
                    text = if (currentWeek != null) todayDayOfWeekString else "$todayDateString $todayDayOfWeekString",
                    fontSizeDp = (10f * finalScale).dp,
                    color = WidgetColors.textHint,
                    modifier = GlanceModifier.wrapContentSize()
                )

                Spacer(modifier = GlanceModifier.defaultWeight())
                // 右侧
                if (currentWeek != null) {
                    ScaledBitmapText(
                        text = context.getString(R.string.status_current_week_format, currentWeek),
                        fontSizeDp = (10f * finalScale).dp,
                        color = WidgetColors.textHint,
                        modifier = GlanceModifier.wrapContentSize()
                    )
                }
            }

            // 主要内容区域
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(horizontal = (6 * finalScale).dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isVacation) {
                    TinyVacationLayout(scale = finalScale)
                } else if (courses.isEmpty()) {
                    TinyNoCoursesLayout(scale = finalScale)
                } else {
                    if (nextCourse != null) {
                        // 课程信息
                        TinyNextCourseContent(nextCourse = nextCourse, scale = finalScale)
                    } else {
                        // 今日课程已全部结束
                        Column(
                            modifier = GlanceModifier.fillMaxSize(),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            ScaledBitmapText(
                                text = context.getString(R.string.widget_today_courses_finished),
                                fontSizeDp = (12f * finalScale).dp,
                                color = WidgetColors.textPrimary,
                                modifier = GlanceModifier.wrapContentSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 下一节课内容（位于主槽位中）
 */
@Composable
fun TinyNextCourseContent(nextCourse: WidgetCourse, scale: Float) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = (2 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧彩色指示条
        Box(
            modifier = GlanceModifier
                .width((2 * scale).dp)
                .fillMaxHeight()
                .background(WidgetColors.courseIndicator1),
            content = {}
        )

        Spacer(modifier = GlanceModifier.width((6 * scale).dp))

        // 右侧课程内容
        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            // 课程名称
            ScaledBitmapText(
                text = nextCourse.name,
                fontSizeDp = (14f * scale).dp,
                color = WidgetColors.textPrimary,
                modifier = GlanceModifier.fillMaxWidth()
            )

            Spacer(modifier = GlanceModifier.height((1 * scale).dp))

            // 时间和地点在同一行
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 时间段
                ScaledBitmapText(
                    text = "${nextCourse.startTime.take(5)}-${nextCourse.endTime.take(5)}",
                    fontSizeDp = (11f * scale).dp,
                    color = WidgetColors.textTertiary,
                    modifier = GlanceModifier.wrapContentSize()
                )

                // 地点信息
                if (nextCourse.position.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.width((4 * scale).dp))
                    ScaledBitmapText(
                        text = nextCourse.position,
                        fontSizeDp = (11f * scale).dp,
                        color = WidgetColors.textTertiary,
                        modifier = GlanceModifier.wrapContentSize()
                    )
                }
            }
        }
    }
}


/**
 * 假期布局
 */
@Composable
fun TinyVacationLayout(scale: Float) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding((10 * scale).dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // ✅ 替换硬编码 "假期中"
        ScaledBitmapText(
            text = context.getString(R.string.status_on_vacation),
            fontSizeDp = (12f * scale).dp,
            color = WidgetColors.textPrimary,
            modifier = GlanceModifier.wrapContentSize()
        )
    }
}

/**
 * 无课程布局
 */
@Composable
fun TinyNoCoursesLayout(scale: Float) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        ScaledBitmapText(
            text = context.getString(R.string.text_no_courses_today),
            fontSizeDp = (12f * scale).dp,
            color = WidgetColors.textPrimary,
            modifier = GlanceModifier.wrapContentSize()
        )
    }
}