package com.xingheyuzhuan.shiguangschedule.widget.moderate

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
import java.util.Locale
import java.time.format.TextStyle as LocalDateTextStyle

// 采用 DoubleDaysLayout 的基准尺寸
private const val BASE_WIDGET_WIDTH = 300f
private const val BASE_WIDGET_HEIGHT = 170f
private const val MAX_LAYOUT_SCALE = 4.0f

// 固定槽位数量
private const val MAX_SLOTS = 4

/**
 * 适中课程小组件的布局：展示今天的 4 节课，支持自动切换到明日预告。
 */
@Composable
fun ModerateLayout(multiDayCoursesAndWeekFlow: Flow<Pair<List<List<WidgetCourse>>, Int?>>) {
    val context = LocalContext.current
    val currentSize = LocalSize.current

    // 1. 统一缩放计算逻辑
    val widthScale = currentSize.width.value / BASE_WIDGET_WIDTH
    val heightScale = currentSize.height.value / BASE_WIDGET_HEIGHT
    val rawScale = minOf(widthScale, heightScale)
    val finalScale = rawScale.coerceIn(1.0f, MAX_LAYOUT_SCALE)

    // 2. 状态和数据
    val coursesAndWeekState = multiDayCoursesAndWeekFlow.collectAsState(initial = Pair(emptyList(), null))
    val (allCoursesLists, currentWeek) = coursesAndWeekState.value

    val todayCourses = allCoursesLists.firstOrNull() ?: emptyList()
    val tomorrowCourses = allCoursesLists.getOrNull(1) ?: emptyList()

    val isVacation = currentWeek == null
    val now = LocalTime.now()

    // 今天剩余未上课程总数
    val remainingTodayCoursesCount = todayCourses.count {
        !it.isSkipped && LocalTime.parse(it.endTime) > now
    }

    val isTodayFinished = remainingTodayCoursesCount == 0
    val hasTomorrowCourses = tomorrowCourses.isNotEmpty()

    val isShowingTomorrow = !isVacation && (todayCourses.isEmpty() || isTodayFinished) && hasTomorrowCourses

    val displayCourses = if (isShowingTomorrow) tomorrowCourses else todayCourses
    val displayDate = if (isShowingTomorrow) LocalDate.now().plusDays(1) else LocalDate.now()

    val nextCourses = if (isShowingTomorrow) {
        displayCourses.filter { !it.isSkipped }.take(MAX_SLOTS)
    } else {
        displayCourses.filter {
            !it.isSkipped && LocalTime.parse(it.endTime) > now
        }.take(MAX_SLOTS)
    }

    val courseSlots = nextCourses + List(MAX_SLOTS - nextCourses.size) { null }

    // 剩余课程数
    val displayRemainingCount = if (isShowingTomorrow) {
        displayCourses.count { !it.isSkipped } // 明天是总课数
    } else {
        remainingTodayCoursesCount // 今天是剩余课数
    }

    // 检查是否应该显示居中状态文本
    val shouldShowCenterStatusText = !isVacation && (
            displayCourses.isEmpty()
                    || (!isShowingTomorrow && nextCourses.isEmpty())
            )


    // 确定顶部文本
    val topText = if (isShowingTomorrow) {
        context.getString(R.string.widget_tomorrow_course_preview)
    } else {
        displayDate.format(DateTimeFormatter.ofPattern("M.d", Locale.getDefault()))
    }
    val subTopText = displayDate.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())

    // 计算缩放后的系统圆角
    val systemCornerRadius = (21 * finalScale).dp

    // 整体布局容器
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
            TopBar(
                mainText = topText,
                subText = subTopText,
                currentWeek = currentWeek,
                scale = finalScale
            )

            // 主要内容区域
            if (isVacation) {
                CenteredMessage(
                    text = context.getString(R.string.title_vacation),
                    scale = finalScale
                )
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
                CenteredMessage(
                    text = statusText,
                    scale = finalScale
                )
            } else {
                CourseGrid(
                    courseSlots = courseSlots,
                    displayRemainingCount = displayRemainingCount,
                    isShowingTomorrow = isShowingTomorrow,
                    scale = finalScale
                )
            }
        }
    }
}

/**
 * 顶部信息栏
 */
@Composable
private fun TopBar(
    mainText: String,
    subText: String,
    currentWeek: Int?,
    scale: Float
) {
    val context = LocalContext.current
    val safeMaxWidth = (100 * scale).dp

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = (8 * scale).dp, vertical = (4 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 日期/预告文本
        EllipsizedBitmapText(
            text = mainText,
            fontSizeDp = (13f * scale).dp,
            color = WidgetColors.textHint,
            maxWidthDp = safeMaxWidth,
            modifier = GlanceModifier.wrapContentSize()
        )
        // 2. 星期/副文本
        if (subText.isNotBlank()) {
            Spacer(modifier = GlanceModifier.width((4 * scale).dp))
            EllipsizedBitmapText(
                text = subText,
                fontSizeDp = (13f * scale).dp,
                color = WidgetColors.textHint,
                maxWidthDp = safeMaxWidth,
                modifier = GlanceModifier.wrapContentSize()
            )
        }


        Spacer(modifier = GlanceModifier.defaultWeight())

        if (currentWeek != null) {
            // 3. 周数文本
            EllipsizedBitmapText(
                text = context.getString(R.string.status_current_week_format, currentWeek),
                fontSizeDp = (13f * scale).dp,
                color = WidgetColors.textHint,
                maxWidthDp = safeMaxWidth,
                modifier = GlanceModifier.wrapContentSize()
            )
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
            maxWidthDp = (250 * scale).dp,
            modifier = GlanceModifier.wrapContentSize()
        )
    }
}

/**
 * 课程网格布局
 */
@Composable
private fun CourseGrid(
    courseSlots: List<WidgetCourse?>,
    displayRemainingCount: Int,
    isShowingTomorrow: Boolean,
    scale: Float
) {
    val context = LocalContext.current

    Column(
        // 使用 fillMaxHeight() 占据父 Column 的剩余空间
        modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        val rows = 2 // 固定 2 行

        // 第一行 (槽位 0, 1)
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.Top
        ) {
            (0 until rows).forEach { colIndex ->
                val slotIndex = colIndex // 0, 1

                Box(modifier = GlanceModifier.defaultWeight()) {
                    CourseGridSlot(
                        course = courseSlots.getOrNull(slotIndex),
                        slotIndex = slotIndex,
                        scale = scale
                    )
                }

                if (colIndex < rows - 1) {
                    Spacer(modifier = GlanceModifier.width((4 * scale).dp)) // 列间距
                }
            }
        }

        // 行间分割线
        RowDivider(scale = scale)

        // 第二行 (槽位 2, 3)
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.Top
        ) {
            (0 until rows).forEach { colIndex ->
                val slotIndex = colIndex + 2 // 2, 3

                Box(modifier = GlanceModifier.defaultWeight()) {
                    CourseGridSlot(
                        course = courseSlots.getOrNull(slotIndex),
                        slotIndex = slotIndex,
                        scale = scale
                    )
                }

                if (colIndex < rows - 1) {
                    Spacer(modifier = GlanceModifier.width((4 * scale).dp))
                }
            }
        }

        // 底部提示
        if (displayRemainingCount > 0) {
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
                    text = context.getString(formatResId, displayRemainingCount),
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
 * 单个课程槽位
 */
@Composable
private fun CourseGridSlot(
    course: WidgetCourse?,
    slotIndex: Int,
    scale: Float
) {
    Box(
        modifier = GlanceModifier.fillMaxHeight(),
        contentAlignment = Alignment.TopStart
    ) {
        if (course != null) {
            CourseItemModerateContent(course = course, index = slotIndex, scale = scale)
        } else {
            // 空槽位
        }
    }
}

/**
 * 课程内容项
 */
@Composable
private fun CourseItemModerateContent(course: WidgetCourse, index: Int, scale: Float) {
    val totalWidgetWidthValue = LocalSize.current.width.value
    val singleColumnAvailableWidth = (totalWidgetWidthValue - (4 * scale)) / 2f
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
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.Horizontal.Start,
        ) {
            // 课程名称
            EllipsizedBitmapText(
                text = course.name,
                fontSizeDp = (13f * scale).dp,
                color = WidgetColors.textPrimary,
                maxWidthDp = calculatedMaxWidth,
                modifier = GlanceModifier.fillMaxWidth()
            )

            // 教师信息
            if (course.teacher.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height((1 * scale).dp))
                EllipsizedBitmapText(
                    text = course.teacher,
                    fontSizeDp = (11f * scale).dp,
                    color = WidgetColors.textSecondary,
                    maxWidthDp = calculatedMaxWidth,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }

            // 时间和地点
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val safeTimeMaxWidth = (90 * scale).dp

                EllipsizedBitmapText(
                    text = "${course.startTime.take(5)}-${course.endTime.take(5)}",
                    fontSizeDp = (11f * scale).dp,
                    color = WidgetColors.textTertiary,
                    maxWidthDp = safeTimeMaxWidth,
                    modifier = GlanceModifier.wrapContentSize()
                )

                if (course.position.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    EllipsizedBitmapText(
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

/**
 * 网格行间分割线
 */
@Composable
private fun RowDivider(scale: Float) {
    Column(
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        Spacer(modifier = GlanceModifier.height((2 * scale).dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = (6 * scale).dp)
        ) {
            // 分割线起点的Spacer：包括指示条宽度(4dp)和指示条与内容之间的间隔(8dp)
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