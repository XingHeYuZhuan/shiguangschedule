package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.clickable
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
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridDefaults.getDarkerColor

/**
 * 冲突课程列表底部动作条。
 *
 * @param courses 冲突的课程列表。
 * @param timeSlots 时间段列表，用于查找开始和结束时间。
 * @param onCourseClicked 当用户点击某个冲突课程时触发的回调。
 * @param onDismissRequest 当底部动作条被关闭时触发的回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictCourseBottomSheet(
    courses: List<CourseWithWeeks>,
    timeSlots: List<TimeSlot>,
    onCourseClicked: (CourseWithWeeks) -> Unit,
    onDismissRequest: () -> Unit
) {
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
            Text(
                text = "课程冲突",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
                color = Color(0xFFFF6F00) // 冲突标题颜色
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(courses) { courseWithWeeks ->
                    val course = courseWithWeeks.course
                    val startSlot = timeSlots.find { it.number == course.startSection }?.startTime ?: "N/A"
                    val endSlot = timeSlots.find { it.number == course.endSection }?.endTime ?: "N/A"

                    // 从课程数据中获取颜色，并应用配置文件中的透明度
                    val cardColor = course.colorInt?.let { Color(it).copy(alpha = ScheduleGridDefaults.CourseBlockAlpha) }
                        ?: ScheduleGridDefaults.DefaultCourseColor.copy(alpha = ScheduleGridDefaults.CourseBlockAlpha)

                    // 根据卡片颜色计算文本颜色，使用配置文件中的变深因子
                    //val textColor = getDarkerColor(cardColor, factor = ScheduleGridDefaults.TextDarkenFactor)
                    val textColor = MaterialTheme.colorScheme.onSurface

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCourseClicked(courseWithWeeks) },
                        colors = CardDefaults.cardColors(containerColor = cardColor), // 应用新的卡片背景色
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
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
                                color = textColor // 应用新的文本颜色
                            )
                            Spacer(Modifier.height(8.dp))
                            // 详细信息
                            Text(
                                text = "时间: 第${course.startSection}-${course.endSection}节 ($startSlot-$endSlot)",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor // 应用新的文本颜色
                            )
                            Text(
                                text = "地点: ${course.position}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor // 应用新的文本颜色
                            )
                            Text(
                                text = "老师: ${course.teacher}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor // 应用新的文本颜色
                            )
                        }
                    }
                }
            }
        }
    }
}