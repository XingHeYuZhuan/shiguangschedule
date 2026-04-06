package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import androidx.compose.ui.res.stringResource
import com.xingheyuzhuan.shiguangschedule.R
import androidx.compose.ui.draw.drawBehind

/**
 * 重叠课程列表底部动作条。
 * 展示同一位置下的所有课程（包含本周与非本周）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlapCourseBottomSheet(
    courses: List<CourseWithWeeks>,
    timeSlots: List<TimeSlot>,
    style: ScheduleGridStyleComposed,
    currentWeek: Int?,
    onCourseClicked: (CourseWithWeeks) -> Unit,
    onDismissRequest: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()


    val fallbackColorAdapted = if (isDarkTheme) {
        style.courseColorMaps.first().dark
    } else {
        style.courseColorMaps.first().light
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题：重叠课程
            Text(
                text = stringResource(R.string.title_course_overlap),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(courses) { courseWithWeeks ->
                    val course = courseWithWeeks.course

                    // 判断该具体课程是否属于本周
                    val isCurrentWeek = currentWeek?.let { cw ->
                        courseWithWeeks.weeks.any { it.weekNumber == cw }
                    } ?: true

                    val isCustomTimeCourse = course.customStartTime != null && course.customEndTime != null
                    val startSlot = timeSlots.find { it.number == course.startSection }?.startTime ?: "N/A"
                    val endSlot = timeSlots.find { it.number == course.endSection }?.endTime ?: "N/A"

                    val colorIndex = course.colorInt.takeIf { it in style.courseColorMaps.indices }
                    val cardBaseColor = colorIndex?.let { index ->
                        val dualColor = style.courseColorMaps[index]
                        if (isDarkTheme) dualColor.dark else dualColor.light
                    } ?: fallbackColorAdapted

                    // 移除透明度调节，始终为 1
                    val cardColor = cardBaseColor
                    val textColor = MaterialTheme.colorScheme.onSurface

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCourseClicked(courseWithWeeks) },
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        // 使用 Box 实现蒙版层叠加在内容层之上
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // 课程名称
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textColor
                                )
                                Spacer(Modifier.height(8.dp))

                                // 时间信息
                                if (isCustomTimeCourse) {
                                    Text(
                                        text = "${course.customStartTime} - ${course.customEndTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = stringResource(
                                            R.string.course_time_description,
                                            course.startSection ?: 0,
                                            course.endSection ?: 0,
                                            startSlot,
                                            endSlot
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor
                                    )
                                }

                                // 地点
                                Text(
                                    text = stringResource(R.string.course_position_prefix, course.position),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor
                                )

                                // 老师
                                Text(
                                    text = stringResource(R.string.course_teacher_prefix, course.teacher),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor
                                )
                            }

                            // 视觉降级蒙版层
                            if (!isCurrentWeek) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            color = (if (isDarkTheme) Color.Black else Color.White)
                                                .copy(alpha = 0.618f)
                                        )
                                        .drawBehind {
                                            val stripeWidth = 5.dp.toPx()
                                            val stripeColor = (if (isDarkTheme) Color.White else Color.Black)
                                                .copy(alpha = 0.06f)
                                            val brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                0.0f to stripeColor,
                                                0.45f to stripeColor,
                                                0.55f to Color.Transparent,
                                                1.0f to Color.Transparent,
                                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                end = androidx.compose.ui.geometry.Offset(stripeWidth, stripeWidth),
                                                tileMode = androidx.compose.ui.graphics.TileMode.Repeated
                                            )
                                            drawRect(brush = brush)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}