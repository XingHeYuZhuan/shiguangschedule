package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.schedule.MergedCourseBlock

/**
 * 绘图模型接口
 * startSection/endSection 基于逻辑节次坐标（0.0 代表第一节课顶部）
 */
interface ISchedulable {
    val columnIndex: Int
    val startSection: Float
    val endSection: Float
    val rawData: MergedCourseBlock
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun ScheduleGrid(
    style: ScheduleGridStyleComposed,
    dates: List<String>,
    currentYear: String,
    timeSlots: List<TimeSlot>,
    mergedCourses: List<MergedCourseBlock>,
    showWeekends: Boolean,
    todayIndex: Int,
    firstDayOfWeek: Int,
    currentSectionIndex: Int = -1,
    onCourseBlockClicked: (MergedCourseBlock) -> Unit,
    onGridCellClicked: (Int, Int) -> Unit,
    onTimeSlotClicked: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenWidth = maxWidth

        // 1. 处理日期与星期排序
        val weekDays = stringArrayResource(R.array.week_days_short_names).toList()
        val reorderedWeekDays = rearrangeDays(weekDays, firstDayOfWeek)
        val displayDays = if (showWeekends) reorderedWeekDays else reorderedWeekDays.take(5)

        // 2. 计算尺寸
        val cellWidth = (screenWidth - style.timeColumnWidth) / displayDays.size
        val totalGridHeight = style.sectionHeight * timeSlots.size
        val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

        // 3. 转换绘图数据
        val schedulables = mergedCourses.mapNotNull { block ->
            val displayIdx = mapDayToDisplayIndex(block.day, firstDayOfWeek, showWeekends)
            if (displayIdx == -1) return@mapNotNull null
            object : ISchedulable {
                override val columnIndex = displayIdx
                override val startSection = block.startSection
                override val endSection = block.endSection
                override val rawData = block
            }
        }

        Column(Modifier.fillMaxSize()) {
            DayHeader(style, displayDays, dates, currentYear, todayIndex, gridLineColor)

            Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                TimeColumn(style, timeSlots, onTimeSlotClicked, Modifier.height(totalGridHeight), gridLineColor, currentSectionIndex)

                Box(Modifier.height(totalGridHeight).weight(1f)) {
                    ClickableGrid(
                        dayCount = displayDays.size,
                        slotCount = timeSlots.size,
                        sectionHeight = style.sectionHeight,
                        lineColor = if (style.hideGridLines) Color.Transparent else gridLineColor
                    ) { dayIdx, sec ->
                        onGridCellClicked(mapDisplayIndexToDay(dayIdx, firstDayOfWeek), sec)
                    }

                    schedulables.forEach { item ->
                        val topOffset = item.startSection * style.sectionHeight
                        val blockHeight = (item.endSection - item.startSection) * style.sectionHeight

                        Box(
                            modifier = Modifier
                                .offset(x = item.columnIndex * cellWidth, y = topOffset)
                                .size(width = cellWidth, height = blockHeight)
                                .padding(style.courseBlockOuterPadding)
                                .clickable { onCourseBlockClicked(item.rawData) }
                        ) {
                            CourseBlock(
                                mergedBlock = item.rawData,
                                style = style,
                                startTime = item.rawData.courses.firstOrNull()?.course?.let {
                                    if(it.isCustomTime) it.customStartTime
                                    else timeSlots.find { ts -> ts.number == it.startSection }?.startTime
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 子组件

@Composable
private fun DayHeader(style: ScheduleGridStyleComposed, displayDays: List<String>, dates: List<String>, currentYear: String, todayIndex: Int, lineColor: Color) {
    Row(Modifier.fillMaxWidth().height(style.dayHeaderHeight)) {
        Box(
            Modifier
                .width(style.timeColumnWidth)
                .fillMaxHeight()
                .drawBehind {
                    if (!style.hideGridLines) {
                        // 右侧线与底部线
                        drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), 1f)
                        drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentYear,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        displayDays.forEachIndexed { index, day ->
            Box(Modifier.weight(1f).fillMaxHeight()
                .background(if (index == todayIndex) MaterialTheme.colorScheme.primaryContainer.copy(0.4f) else Color.Transparent)
                .drawBehind {
                    if (!style.hideGridLines) {
                        drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), 1f)
                        drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                    }
                }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 统一使用 onSurface，不加高亮判断
                    Text(
                        text = day,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!style.hideDateUnderDay && dates.size > index) {
                        Text(
                            text = dates[index],
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeColumn(style: ScheduleGridStyleComposed, timeSlots: List<TimeSlot>, onTimeSlotClicked: () -> Unit, modifier: Modifier, lineColor: Color, currentSectionIndex: Int = -1) {
    Column(modifier.width(style.timeColumnWidth)) {
        timeSlots.forEachIndexed { index, slot ->
            val isCurrentSection = index + 1 == currentSectionIndex
            val backgroundColor = if (isCurrentSection) {
                MaterialTheme.colorScheme.primaryContainer.copy(0.4f)
            } else {
                Color.Transparent
            }
            val textColor = if (isCurrentSection) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val timeColor = if (isCurrentSection) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(style.sectionHeight)
                    .clickable { onTimeSlotClicked() }
                    .background(backgroundColor)
                    .drawBehind {
                        if (!style.hideGridLines) {
                            drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), 1f)
                            drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = slot.number.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (!style.hideSectionTime) {
                    Text(
                        text = slot.startTime,
                        fontSize = 10.sp,
                        color = timeColor
                    )
                    Text(
                        text = slot.endTime,
                        fontSize = 10.sp,
                        color = timeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ClickableGrid(dayCount: Int, slotCount: Int, sectionHeight: Dp, lineColor: Color, onClick: (Int, Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        for (sec in 1..slotCount) {
            Row(Modifier.fillMaxWidth().height(sectionHeight)) {
                repeat(dayCount) { idx ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .drawBehind {
                                // 绘制逻辑间隔线
                                drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), 1f)
                                drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                            }
                            .clickable { onClick(idx, sec) }
                    )
                }
            }
        }
    }
}

//  辅助逻辑

private fun rearrangeDays(originalDays: List<String>, firstDayOfWeek: Int): List<String> {
    val startIndex = (firstDayOfWeek - 1).coerceIn(0, 6)
    return originalDays.subList(startIndex, originalDays.size) + originalDays.subList(0, startIndex)
}

private fun mapDayToDisplayIndex(courseDay: Int, firstDayOfWeek: Int, showWeekends: Boolean): Int {
    val idx = (courseDay - firstDayOfWeek + 7) % 7
    return if (idx >= if (showWeekends) 7 else 5) -1 else idx
}

private fun mapDisplayIndexToDay(idx: Int, firstDayOfWeek: Int): Int = (firstDayOfWeek - 1 + idx) % 7 + 1