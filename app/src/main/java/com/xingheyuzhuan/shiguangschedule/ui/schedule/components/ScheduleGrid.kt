package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.schedule.MergedCourseBlock

/**
 * 渲染课表网格的 UI 组件。
 * 它不关心数据逻辑，只负责根据传入的数据进行绘制。
 */
@Composable
fun ScheduleGrid(
    dates: List<String>,
    timeSlots: List<TimeSlot>,
    mergedCourses: List<MergedCourseBlock>,
    showWeekends: Boolean,
    todayIndex: Int,
    onCourseBlockClicked: (MergedCourseBlock) -> Unit,
    onGridCellClicked: (Int, Int) -> Unit,
    onTimeSlotClicked: () -> Unit
) {
    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
    val displayDays = if (showWeekends) weekDays else weekDays.take(5)
    val displayDayCount = displayDays.size

    val screenWidth = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    val timeColumnWidth = ScheduleGridDefaults.TimeColumnWidth
    val dayHeaderHeight = ScheduleGridDefaults.DayHeaderHeight
    val sectionHeight = ScheduleGridDefaults.SectionHeight
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    val gridWidth = screenWidth - timeColumnWidth
    val cellWidth = gridWidth / displayDayCount

    val timeSlotsCount = timeSlots.size
    val totalGridContentHeight = timeSlotsCount * sectionHeight

    Column(modifier = Modifier.fillMaxSize()) {
        DayHeader(
            displayDays = displayDays,
            dates = dates,
            cellWidth = cellWidth,
            timeColumnWidth = timeColumnWidth,
            dayHeaderHeight = dayHeaderHeight,
            todayIndex = todayIndex,
            gridLineColor = gridLineColor
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TimeColumn(
                timeSlots = timeSlots,
                timeColumnWidth = timeColumnWidth,
                sectionHeight = sectionHeight,
                onTimeSlotClicked = onTimeSlotClicked,
                modifier = Modifier.height(totalGridContentHeight)
            )

            // 网格和课程层，使用 Box 来堆叠
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // 背景层：网格线
                GridLines(
                    dayCount = displayDayCount,
                    timeSlotsCount = timeSlotsCount,
                    cellWidth = cellWidth,
                    gridHeight = totalGridContentHeight,
                    sectionHeight = sectionHeight,
                    gridLineColor = gridLineColor
                )

                // 可点击的空白区域
                ClickableGrid(
                    dayCount = displayDayCount,
                    timeSlotsCount = timeSlotsCount,
                    sectionHeight = sectionHeight,
                    onGridCellClicked = onGridCellClicked
                )

                // 浮动层：课程块渲染
                mergedCourses.forEach { mergedBlock ->
                    val offsetX = (mergedBlock.day - 1) * cellWidth
                    val offsetY = (mergedBlock.startSection - 1) * sectionHeight
                    val blockWidth = cellWidth
                    val blockHeight = (mergedBlock.endSection - mergedBlock.startSection + 1) * sectionHeight

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .size(width = blockWidth, height = blockHeight)
                            .clickable { onCourseBlockClicked(mergedBlock) }
                    ) {
                        CourseBlock(
                            mergedBlock = mergedBlock
                        )
                    }
                }
            }
        }
    }
}

/**
 * 星期栏 UI 组件。
 */
@Composable
private fun DayHeader(
    displayDays: List<String>,
    dates: List<String>,
    cellWidth: Dp,
    timeColumnWidth: Dp,
    dayHeaderHeight: Dp,
    todayIndex: Int,
    gridLineColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dayHeaderHeight)
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawLine(
                    color = gridLineColor.copy(alpha = 0.3f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
    ) {
        Spacer(
            modifier = Modifier
                .width(timeColumnWidth)
                .fillMaxHeight()
        )

        // 星期几和日期
        displayDays.forEachIndexed { index, day ->
            val isToday = index == todayIndex
            val backgroundColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            val textColor = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            val dateColor = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

            Column(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxHeight()
                    .background(backgroundColor)
                    .drawBehind {
                        if (index < displayDays.size - 1) {
                            drawLine(
                                color = gridLineColor.copy(alpha = 0.3f),
                                start = Offset(size.width, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "周$day",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = 14.sp
                )
                if (dates.size > index) {
                    Text(
                        text = dates[index],
                        fontSize = 10.sp,
                        color = dateColor,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * 左侧时间列表 UI 组件。
 */
@Composable
private fun TimeColumn(
    timeSlots: List<TimeSlot>,
    timeColumnWidth: Dp,
    sectionHeight: Dp,
    onTimeSlotClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .width(timeColumnWidth)
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                val strokeWidth = 1f
                val transparentColor = gridLineColor.copy(alpha = 0.3f)

                // 绘制右侧的垂直分割线
                drawLine(
                    color = transparentColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )

                // 绘制所有横向分割线
                for (i in 0..timeSlots.size) {
                    val startY = i * sectionHeight.toPx()
                    drawLine(
                        color = transparentColor,
                        start = Offset(0f, startY),
                        end = Offset(size.width, startY),
                        strokeWidth = strokeWidth
                    )
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        timeSlots.forEach { slot ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sectionHeight)
                    .clickable {
                        onTimeSlotClicked()
                    }
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = slot.number.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                Text(text = slot.startTime, fontSize = 10.sp, lineHeight = 10.sp)
                Text(text = slot.endTime, fontSize = 10.sp, lineHeight = 10.sp)
            }
        }
    }
}

/**
 * 网格线 UI 组件。
 */
@Composable
private fun GridLines(
    dayCount: Int,
    timeSlotsCount: Int,
    cellWidth: Dp,
    gridHeight: Dp,
    sectionHeight: Dp,
    gridLineColor: Color
) {
    val strokeWidth = 1f
    val transparentColor = gridLineColor.copy(alpha = 0.3f)

    // Canvas 填充父级 Box，由父级 Box 决定高度
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        // 绘制竖线
        for (i in 1..dayCount) {
            val startX = i * cellWidth.toPx()
            drawLine(
                color = transparentColor,
                start = Offset(startX, 0f),
                end = Offset(startX, gridHeight.toPx()),
                strokeWidth = strokeWidth
            )
        }

        // 绘制横线
        for (i in 0..timeSlotsCount) {
            val startY = i * sectionHeight.toPx()
            drawLine(
                color = transparentColor,
                start = Offset(0f, startY),
                end = Offset(size.width, startY),
                strokeWidth = strokeWidth
            )
        }
    }
}

/**
 * 可点击的空白区域。
 */
@Composable
private fun ClickableGrid(
    dayCount: Int,
    timeSlotsCount: Int,
    sectionHeight: Dp,
    onGridCellClicked: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        for (section in 1..timeSlotsCount) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(sectionHeight)) {
                for (day in 1..dayCount) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                onGridCellClicked(day, section)
                            }
                    )
                }
            }
        }
    }
}