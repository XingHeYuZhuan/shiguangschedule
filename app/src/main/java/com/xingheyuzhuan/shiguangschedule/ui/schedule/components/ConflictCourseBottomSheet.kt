package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import androidx.compose.ui.res.stringResource
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.COURSE_COLOR_MAPS

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
    val isDarkTheme = isSystemInDarkTheme()

    val conflictTitleColor = if (isDarkTheme) {
        ScheduleGridDefaults.ConflictCourseColorDark
    } else {
        ScheduleGridDefaults.ConflictCourseColor
    }

    val fallbackColorAdapted = if (isDarkTheme) {
        COURSE_COLOR_MAPS.first().dark
    } else {
        COURSE_COLOR_MAPS.first().light
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
            Text(
                text = stringResource(R.string.title_course_conflict),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
                color = conflictTitleColor
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

                    val colorIndex = course.colorInt.takeIf { it in COURSE_COLOR_MAPS.indices }

                    val cardBaseColor = colorIndex?.let { index ->
                        val dualColor = COURSE_COLOR_MAPS[index]
                        if (isDarkTheme) dualColor.dark else dualColor.light
                    } ?: fallbackColorAdapted

                    // 应用配置文件中的透明度
                    val cardColor = cardBaseColor.copy(alpha = ScheduleGridDefaults.CourseBlockAlpha)

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
                                text = stringResource(
                                    R.string.course_time_description,
                                    course.startSection, // 对应 %1$s (起始节次)
                                    course.endSection,   // 对应 %2$s (结束节次)
                                    startSlot,           // 对应 %3$s (起始时间)
                                    endSlot              // 对应 %4$s (结束时间)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                            // 详细信息 - 地点
                            Text(
                                text = stringResource(
                                    R.string.course_position_prefix,
                                    course.position
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                            // 详细信息 - 老师
                            Text(
                                text = stringResource(
                                    R.string.course_teacher_prefix,
                                    course.teacher
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}