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
import androidx.compose.ui.res.stringArrayResource
import com.xingheyuzhuan.shiguangschedule.R

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
    firstDayOfWeek: Int,
    onCourseBlockClicked: (MergedCourseBlock) -> Unit,
    onGridCellClicked: (Int, Int) -> Unit,
    onTimeSlotClicked: () -> Unit
) {
    val weekDays = stringArrayResource(R.array.week_days_full_names).toList()

    val reorderedWeekDays = rearrangeDays(weekDays, firstDayOfWeek)

    val displayDays = if (showWeekends) reorderedWeekDays else reorderedWeekDays.take(5)
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
                    onGridCellClicked = { displayIndex, section ->
                        val originalDay = mapDisplayIndexToDay(displayIndex, firstDayOfWeek)
                        onGridCellClicked(originalDay, section)
                    }
                )

                // 浮动层：课程块渲染
                mergedCourses.forEach { mergedBlock ->
                    val newDayIndex = mapDayToDisplayIndex(mergedBlock.day, firstDayOfWeek, showWeekends)

                    // 只有当课程在显示的列范围内时才绘制
                    if (newDayIndex != -1) {
                        // 使用 0-based 的索引计算 offsetX
                        val offsetX = newDayIndex * cellWidth
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
                    text = day,
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
                // 【修改 5.1：遍历 0-based 索引】
                for (displayIndex in 0 until dayCount) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                onGridCellClicked(displayIndex, section)
                            }
                    )
                }
            }
        }
    }
}


/**
 * 根据起始日重新排列星期列表。
 * @param originalDays 原始星期列表 (周一, 周二, ..., 周日)
 * @param firstDayOfWeek 一周的起始日 (1=周一, 7=周日)
 * @return 排列后的星期列表
 */
private fun rearrangeDays(originalDays: List<String>, firstDayOfWeek: Int): List<String> {
    val startIndex = firstDayOfWeek - 1 // 转换为 0-based 索引 (0=周一, 6=周日)
    if (startIndex == 0) return originalDays

    // 将列表分成两部分并交换位置
    val part1 = originalDays.subList(startIndex, originalDays.size)
    val part2 = originalDays.subList(0, startIndex)
    return part1 + part2
}

/**
 * 将课程的 Day (1-7) 映射到当前网格的显示索引 (0-N)。
 * @param courseDay 课程的星期几 (1=周一, 7=周日)
 * @param firstDayOfWeek 一周的起始日 (1=周一, 7=周日)
 * @param showWeekends 是否显示周末
 * @return 在显示列表中的 0-based 索引，如果不在显示范围内则返回 -1
 */
private fun mapDayToDisplayIndex(courseDay: Int, firstDayOfWeek: Int, showWeekends: Boolean): Int {
    val totalDays = if (showWeekends) 7 else 5

    // 1. 计算理论上的 0-based 索引
    // (courseDay - firstDayOfWeek) 给出相对于起始日的偏移量，
    // + 7 确保结果为正，% 7 得到最终的 0-6 索引。
    val theoreticalIndex = (courseDay - firstDayOfWeek + 7) % 7

    // 2. 检查是否在显示的列数范围内
    if (theoreticalIndex >= totalDays) {
        return -1
    }

    return theoreticalIndex
}

/**
 * 将网格的显示索引 (0-N) 映射回课程 DayOfWeek (1-7)。
 * 用于 onGridCellClicked 回调。
 */
private fun mapDisplayIndexToDay(displayIndex: Int, firstDayOfWeek: Int): Int {
    // 1. 计算理论上的 1-based 索引
    // (firstDayOfWeek - 1) 是起始日的 0-based 索引
    // (firstDayOfWeek - 1 + displayIndex) 是目标日期的 0-based 索引
    // % 7 得到 0-6 循环索引
    // + 1 转换回 1-7 DayOfWeek
    val day = (firstDayOfWeek - 1 + displayIndex) % 7 + 1
    return day
}