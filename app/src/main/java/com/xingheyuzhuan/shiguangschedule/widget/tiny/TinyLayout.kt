package com.xingheyuzhuan.shiguangschedule.widget.tiny

import android.graphics.Paint
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.widget.EllipsizedBitmapText
import com.xingheyuzhuan.shiguangschedule.widget.WidgetColors
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

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

    val isVacation = currentWeek == null
    val now = LocalTime.now()

    val nextCourse = courses.firstOrNull {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }

    // 缩放圆角半径和统一内边距
    val systemCornerRadius = (21 * finalScale).dp
    val primaryPadding = (8 * finalScale).dp

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
            // 主要内容区域：占据所有空间
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(horizontal = primaryPadding, vertical = (10 * finalScale).dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isVacation) {
                    TinyVacationLayout(scale = finalScale)
                } else if (courses.isEmpty()) {
                    TinyNoCoursesLayout(scale = finalScale)
                } else {
                    if (nextCourse != null) {
                        TinyNextCourseContent(
                            nextCourse = nextCourse,
                            courses = courses,
                            scale = finalScale
                        )
                    } else {
                        val widgetWidth = LocalSize.current.width

                        Column(
                            modifier = GlanceModifier.fillMaxSize(),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            EllipsizedBitmapText(
                                text = context.getString(R.string.widget_today_courses_finished),
                                fontSizeDp = (12f * finalScale).dp,
                                color = WidgetColors.textPrimary,
                                maxWidthDp = widgetWidth,
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
 * 下一节课内容：三行垂直布局
 * @param courses 今日全部课程列表，用于计算剩余课程数。
 */
@Composable
fun TinyNextCourseContent(nextCourse: WidgetCourse, courses: List<WidgetCourse>, scale: Float) {
    val totalWidgetWidthValue = LocalSize.current.width.value

    // 1. 计算剩余课程数量
    val nextCourseIndex = courses.indexOf(nextCourse)
    val remainingCourseCount = if (nextCourseIndex != -1) courses.size - nextCourseIndex else 1
    val remainingText = remainingCourseCount.toString()

    // 2. 计算课程内容的可用最大宽度
    val tagSizeValue = 18f
    val paddingValue = 8f * 2
    val spacerValue = 6f
    val calculatedMaxWidth = (totalWidgetWidthValue - (paddingValue + tagSizeValue + spacerValue) * scale).dp

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        // 垂直对齐：让内容和指示点对齐到顶部
        verticalAlignment = Alignment.Top
    ) {
        // 左侧课程内容 - 占据主要空间
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            EllipsizedBitmapText(
                text = nextCourse.name,
                fontSizeDp = (16f * scale).dp,
                color = WidgetColors.textPrimary,
                maxWidthDp = calculatedMaxWidth,
                modifier = GlanceModifier.fillMaxWidth()
            )

            Spacer(modifier = GlanceModifier.height((2 * scale).dp))

            EllipsizedBitmapText(
                text = "${nextCourse.startTime.take(5)}-${nextCourse.endTime.take(5)}",
                fontSizeDp = (12f * scale).dp,
                color = WidgetColors.textPrimary,
                maxWidthDp = calculatedMaxWidth,
                modifier = GlanceModifier.fillMaxWidth()
            )

            if (nextCourse.position.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height((2 * scale).dp))
                EllipsizedBitmapText(
                    text = nextCourse.position,
                    fontSizeDp = (11f * scale).dp,
                    color = WidgetColors.textTertiary,
                    maxWidthDp = calculatedMaxWidth,
                    textAlign = Paint.Align.LEFT,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = GlanceModifier.width((6 * scale).dp))

        val tagSize = (tagSizeValue * scale).dp
        Box(
            modifier = GlanceModifier
                .size(tagSize)
                .background(WidgetColors.courseIndicator1)
                .cornerRadius(tagSize / 2),
            contentAlignment = Alignment.Center
        ) {
            EllipsizedBitmapText(
                text = remainingText,
                fontSizeDp = (11f * scale).dp,
                color = WidgetColors.background,
                maxWidthDp = tagSize,
                textAlign = Paint.Align.CENTER,
                modifier = GlanceModifier.wrapContentSize()
            )
        }
    }
}


/**
 * 假期布局
 */
@Composable
fun TinyVacationLayout(scale: Float) {
    val context = LocalContext.current
    val widgetWidth = LocalSize.current.width

    Column(
        modifier = GlanceModifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        EllipsizedBitmapText(
            text = context.getString(R.string.status_on_vacation),
            fontSizeDp = (14f * scale).dp,
            color = WidgetColors.textPrimary,
            maxWidthDp = widgetWidth,
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
    val widgetWidth = LocalSize.current.width

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        EllipsizedBitmapText(
            text = context.getString(R.string.text_no_courses_today),
            fontSizeDp = (14f * scale).dp,
            color = WidgetColors.textPrimary,
            maxWidthDp = widgetWidth,
            modifier = GlanceModifier.wrapContentSize()
        )
    }
}