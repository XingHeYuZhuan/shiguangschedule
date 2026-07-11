package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.ui.schedule.MergedCourseBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailBottomSheet(
    block: MergedCourseBlock,
    onDismissRequest: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val courseWrapper = block.courses.firstOrNull() ?: return
    val course = courseWrapper.course

    val weeksDisplayStr = remember(courseWrapper.weeks) {
        val sortedWeeks = courseWrapper.weeks.map { it.weekNumber }.sorted()
        if (sortedWeeks.isEmpty()) "" else sortedWeeks.joinToString(", ")
    }

    val weekDaysFullNames = stringArrayResource(id = R.array.week_days_full_names)
    val dayStr = remember(course.day, weekDaysFullNames) {
        weekDaysFullNames.getOrNull(course.day - 1) ?: ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 课程名称
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Class, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(course.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }

                // 教师
                if (course.teacher.isNotBlank()) {
                    DetailItem(Icons.Default.Person, course.teacher)
                }

                // 地点
                if (course.position.isNotBlank()) {
                    DetailItem(Icons.Default.LocationOn, course.position)
                }

                // 周次
                if (weeksDisplayStr.isNotEmpty()) {
                    DetailItem(Icons.Default.CalendarToday, weeksDisplayStr)
                }

                // 星期与具体时间
                val sectionSuffix = stringResource(id = R.string.label_section_range_suffix)
                val timeStr = if (course.isCustomTime) {
                    "${course.customStartTime} - ${course.customEndTime}"
                } else {
                    "${course.startSection ?: 0}-${course.endSection ?: 0} $sectionSuffix"
                }

                DetailItem(Icons.Default.Schedule) {
                    Column {
                        Text(dayStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(timeStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }

                // 备注
                if (!course.remark.isNullOrBlank()) {
                    DetailItem(Icons.AutoMirrored.Filled.Notes, course.remark)
                }
            }

            // 编辑按钮
            FilledIconButton(
                onClick = { onEditClick(course.id) },
                modifier = Modifier.align(Alignment.TopEnd).size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.a11y_edit), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DetailItem(icon: ImageVector, text: String) {
    DetailItem(icon = icon) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailItem(icon: ImageVector, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(top = 2.dp).size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        content()
    }
}