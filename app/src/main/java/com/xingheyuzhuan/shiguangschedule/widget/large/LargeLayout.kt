package com.xingheyuzhuan.shiguangschedule.widget.large

import android.graphics.Paint
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
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.widget.EllipsizedBitmapText
import com.xingheyuzhuan.shiguangschedule.widget.WidgetColors
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as LocalDateTextStyle
import java.util.Locale

// 采用 4x3 的基准尺寸
private const val BASE_WIDGET_WIDTH = 320f
private const val BASE_WIDGET_HEIGHT = 240f
private const val MAX_LAYOUT_SCALE = 4.0f

// 固定槽位数量
private const val MAX_SLOTS = 6

/**
 * 大尺寸课程小组件的布局：展示今天的 6 节课 (2列 x 3行)，支持自动切换到明日预告。
 */
@Composable
fun LargeLayout(multiDayCoursesAndWeekFlow: Flow<Pair<List<List<WidgetCourse>>, Int?>>) {
    val context = LocalContext.current
    val currentSize = LocalSize.current

    val widthScale = currentSize.width.value / BASE_WIDGET_WIDTH
    val heightScale = currentSize.height.value / BASE_WIDGET_HEIGHT
    val rawScale = minOf(widthScale, heightScale)
    val finalScale = rawScale.coerceIn(1.0f, MAX_LAYOUT_SCALE)

    // 2. 状态和数据提取
    val coursesAndWeekState = multiDayCoursesAndWeekFlow.collectAsState(initial = Pair(emptyList(), null))
    val (allCoursesLists, currentWeek) = coursesAndWeekState.value

    val todayCourses = allCoursesLists.firstOrNull() ?: emptyList()
    val tomorrowCourses = allCoursesLists.getOrNull(1) ?: emptyList()

    val isVacation = currentWeek == null
    val now = LocalTime.now()

    // 3. 决定显示哪一天的课程
    val remainingTodayCoursesCount = todayCourses.count {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }
    val isTodayFinished = remainingTodayCoursesCount == 0
    val hasTomorrowCourses = tomorrowCourses.isNotEmpty()

    val isShowingTomorrow = !isVacation && (todayCourses.isEmpty() || isTodayFinished) && hasTomorrowCourses

    val displayCourses = if (isShowingTomorrow) tomorrowCourses else todayCourses
    val displayDate = if (isShowingTomorrow) LocalDate.now().plusDays(1) else LocalDate.now()

    // 4. 计算需要显示的课程列表、剩余数和顶部文本
    val nextCourses = if (isShowingTomorrow) {
        displayCourses.filter { !it.isSkipped }.take(MAX_SLOTS)
    } else {
        displayCourses.filter {
            !it.isSkipped && LocalTime.parse(it.endTime) > now
        }.take(MAX_SLOTS)
    }
    val courseSlots = nextCourses + List(MAX_SLOTS - nextCourses.size) { null }

    val displayRemainingCount = if (isShowingTomorrow) {
        displayCourses.count { !it.isSkipped } // 明天是总课数
    } else {
        remainingTodayCoursesCount // 今天是剩余课数
    }

    val topText = if (isShowingTomorrow) {
        context.getString(R.string.widget_tomorrow_course_preview)
    } else {
        displayDate.format(DateTimeFormatter.ofPattern("M.d", Locale.getDefault()))
    }
    val subTopText = displayDate.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())

    val shouldShowCenterStatusText = !isVacation && (
            displayCourses.isEmpty()
                    || (!isShowingTomorrow && nextCourses.isEmpty())
            )

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
            // 顶部区域：日期、星期（左侧显示）和周数（右侧显示）
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = (8 * finalScale).dp, vertical = (6 * finalScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val safeTextMaxWidth = (150 * finalScale).dp

                val fullLeftText = context.getString(
                    R.string.widget_date_dayofweek_format,
                    topText,
                    subTopText
                )

                EllipsizedBitmapText(
                    text = fullLeftText,
                    fontSizeDp = (12f * finalScale).dp,
                    color = WidgetColors.textHint,
                    maxWidthDp = safeTextMaxWidth,
                    modifier = GlanceModifier.wrapContentSize()
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 右侧周数
                if (currentWeek != null) {
                    EllipsizedBitmapText(
                        text = context.getString(R.string.status_current_week_format, currentWeek),
                        fontSizeDp = (12f * finalScale).dp,
                        color = WidgetColors.textHint,
                        maxWidthDp = safeTextMaxWidth,
                        modifier = GlanceModifier.wrapContentSize()
                    )
                }
            }

            // 主要内容区域：占据所有剩余空间
            if (isVacation) {
                CenteredMessage(text = context.getString(R.string.title_vacation), scale = finalScale)
            } else if (shouldShowCenterStatusText) {
                val statusText = if (displayCourses.isEmpty()) {
                    if (isShowingTomorrow) {
                        context.getString(R.string.widget_no_courses_tomorrow)
                    } else {
                        context.getString(R.string.text_no_courses_today)
                    }
                } else {
                    context.getString(R.string.widget_today_courses_finished)
                }
                CenteredMessage(text = statusText, scale = finalScale)
            } else {
                CourseGridLarge(
                    courseSlots = courseSlots,
                    remainingCoursesCount = displayRemainingCount,
                    isShowingTomorrow = isShowingTomorrow,
                    scale = finalScale
                )
            }
        }
    }
}

/**
 * 居中显示消息
 */
@Composable
private fun CenteredMessage(text: String, scale: Float) {
    Column(
        modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        EllipsizedBitmapText(
            text = text,
            fontSizeDp = (14f * scale).dp,
            color = WidgetColors.textPrimary,
            maxWidthDp = (300 * scale).dp,
            modifier = GlanceModifier.wrapContentSize()
        )
    }
}

/**
 * 课程网格布局 (2列 x 3行)
 */
@Composable
private fun CourseGridLarge(
    courseSlots: List<WidgetCourse?>,
    remainingCoursesCount: Int,
    isShowingTomorrow: Boolean,
    scale: Float
) {
    val context = LocalContext.current
    Column(
        // 主 Column 占据父 Column 的所有剩余空间
        modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        // 网格内容区域：占据 Column 的所有剩余空间
        Column(
            // 占据大部分垂直空间，为底部的提示栏留出位置
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            // 循环 3 次，创建 3 行
            (0 until 3).forEach { rowIndex ->
                val slotIndexLeft = rowIndex * 2 // 0, 2, 4
                val slotIndexRight = rowIndex * 2 + 1 // 1, 3, 5

                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧槽位
                    Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                        CourseGridSlotLarge(
                            course = courseSlots.getOrNull(slotIndexLeft),
                            slotIndex = slotIndexLeft,
                            scale = scale
                        )
                    }

                    Spacer(modifier = GlanceModifier.width((4 * scale).dp))

                    // 右侧槽位
                    Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                        CourseGridSlotLarge(
                            course = courseSlots.getOrNull(slotIndexRight),
                            slotIndex = slotIndexRight,
                            scale = scale
                        )
                    }
                }

                if (rowIndex < 2) {
                    LargeRowDivider(scale = scale)
                }
            }
        }

        // 3. 底部提示区域：统一风格
        if (remainingCoursesCount > 0) {

            val formatResId = if (isShowingTomorrow) {
                R.string.widget_remaining_courses_format_tomorrow
            } else {
                R.string.widget_remaining_courses_format_today
            }

            val safeMaxWidth = (200 * scale).dp

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = (8 * scale).dp)
                    .padding(top = (4 * scale).dp, bottom = (6 * scale).dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                EllipsizedBitmapText(
                    text = context.getString(formatResId, remainingCoursesCount),
                    fontSizeDp = (11f * scale).dp,
                    color = WidgetColors.textHint,
                    maxWidthDp = safeMaxWidth,
                    modifier = GlanceModifier.wrapContentSize()
                )
            }
        }
    }
}

/**
 * 网格行间分割线 (无需改动)
 */
@Composable
private fun LargeRowDivider(scale: Float) {
    Column(
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        Spacer(modifier = GlanceModifier.height((2 * scale).dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = (6 * scale).dp)
        ) {
            Spacer(modifier = GlanceModifier.width((4 * scale).dp + (8 * scale).dp))
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height((1 * scale).dp)
                    .background(WidgetColors.divider)
            ) { /* content lambda 块 */ }
        }
        Spacer(modifier = GlanceModifier.height((2 * scale).dp))
    }
}

/**
 * 单个课程槽位
 */
@Composable
private fun CourseGridSlotLarge(
    course: WidgetCourse?,
    slotIndex: Int,
    scale: Float
) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        if (course != null) {
            CourseItemLargeGrid(course = course, index = slotIndex, scale = scale)
        } else {
            // 空槽位
        }
    }
}

/**
 * 课程内容项
 */
@Composable
fun CourseItemLargeGrid(course: WidgetCourse, index: Int, scale: Float) {
    val totalWidgetWidthValue = LocalSize.current.width.value
    val singleColumnAvailableWidth = (totalWidgetWidthValue - (8 * scale) * 2) / 2f
    val calculatedWidthValue = singleColumnAvailableWidth - (6 * scale) * 2 - (4 * scale) - (8 * scale)
    val calculatedMaxWidth = if (calculatedWidthValue > 1f) calculatedWidthValue.dp else 1.dp

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = (6 * scale).dp)
            .padding(vertical = (4 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧彩色指示条
        Image(
            provider = ImageProvider(getCourseIndicatorDrawable(index)),
            contentDescription = null,
            modifier = GlanceModifier
                .width((4 * scale).dp)
                .fillMaxHeight()
        )

        Spacer(modifier = GlanceModifier.width((8 * scale).dp))

        // 右侧课程内容
        Column(
            modifier = GlanceModifier.defaultWeight(), // 占据剩余空间
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            // 课程名称
            EllipsizedBitmapText(
                text = course.name,
                fontSizeDp = (13f * scale).dp,
                color = WidgetColors.textPrimary,
                maxWidthDp = calculatedMaxWidth,
                modifier = GlanceModifier.fillMaxWidth()
            )
            Spacer(modifier = GlanceModifier.height((3 * scale).dp))

            // 教师信息
            if (course.teacher.isNotBlank()) {
                EllipsizedBitmapText(
                    text = course.teacher,
                    fontSizeDp = (12f * scale).dp,
                    color = WidgetColors.textSecondary,
                    maxWidthDp = calculatedMaxWidth,
                    modifier = GlanceModifier.fillMaxWidth()
                )
                Spacer(modifier = GlanceModifier.height((3 * scale).dp))
            }

            // 时间和地点
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val safeTimeMaxWidth = (100 * scale).dp

                EllipsizedBitmapText( // 替换
                    text = "${course.startTime.take(5)}-${course.endTime.take(5)}",
                    fontSizeDp = (11f * scale).dp,
                    color = WidgetColors.textTertiary,
                    maxWidthDp = safeTimeMaxWidth,
                    modifier = GlanceModifier.wrapContentSize()
                )
                if (course.position.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // 地点：占据剩余所有空间，并右对齐
                    EllipsizedBitmapText( // 替换
                        text = course.position,
                        fontSizeDp = (11f * scale).dp,
                        color = WidgetColors.textTertiary,
                        maxWidthDp = calculatedMaxWidth,
                        textAlign = Paint.Align.RIGHT,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
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