package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.schedule.MergedCourseBlock
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_edit
import shiguangschedule.shared.generated.resources.action_double_week
import shiguangschedule.shared.generated.resources.action_single_week
import shiguangschedule.shared.generated.resources.calendar_today_24px
import shiguangschedule.shared.generated.resources.class_24px
import shiguangschedule.shared.generated.resources.edit_24px
import shiguangschedule.shared.generated.resources.label_section_range_suffix
import shiguangschedule.shared.generated.resources.location_on_24px
import shiguangschedule.shared.generated.resources.person_24px
import shiguangschedule.shared.generated.resources.schedule_24px
import shiguangschedule.shared.generated.resources.sticky_note_2_24px
import shiguangschedule.shared.generated.resources.week_days_full_names

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailBottomSheet(
    block: MergedCourseBlock,
    onDismissRequest: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val courseWrapper = block.courses.firstOrNull() ?: return
    val course = courseWrapper.course

    val weeksDisplayStr = formatWeeks(courseWrapper.weeks.map { it.weekNumber })

    val weekDaysFullNames = stringArrayResource(Res.array.week_days_full_names)
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
                    Icon(vectorResource(Res.drawable.class_24px), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(course.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }

                // 教师
                if (course.teacher.isNotBlank()) {
                    DetailItem(vectorResource(Res.drawable.person_24px), course.teacher)
                }

                // 地点
                if (course.position.isNotBlank()) {
                    DetailItem(vectorResource(Res.drawable.location_on_24px), course.position)
                }

                // 周次
                if (weeksDisplayStr.isNotEmpty()) {
                    DetailItem(vectorResource(Res.drawable.calendar_today_24px), weeksDisplayStr)
                }

                // 星期与具体时间
                val sectionSuffix = stringResource(Res.string.label_section_range_suffix)
                val timeStr = if (course.isCustomTime) {
                    "${course.customStartTime} - ${course.customEndTime}"
                } else {
                    "${course.startSection ?: 0}-${course.endSection ?: 0} $sectionSuffix"
                }

                DetailItem(vectorResource(Res.drawable.schedule_24px)) {
                    Column {
                        Text(dayStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(timeStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }

                // 备注
                val remark = course.remark
                if (!remark.isNullOrBlank()) {
                    DetailItem(vectorResource(Res.drawable.sticky_note_2_24px), remark)
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
                Icon(vectorResource(Res.drawable.edit_24px), contentDescription = stringResource(Res.string.a11y_edit), modifier = Modifier.size(20.dp))
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
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        content()
    }
}

@Composable
private fun formatWeeks(weeks: List<Int>): String {
    if (weeks.isEmpty()) return ""
    val sorted = weeks.distinct().sorted()
    val result = mutableListOf<String>()

    val singleLabel = stringResource(Res.string.action_single_week)
    val doubleLabel = stringResource(Res.string.action_double_week)

    var i = 0
    while (i < sorted.size) {
        // 识别等差序列（单双周）
        if (i + 1 < sorted.size && sorted[i + 1] - sorted[i] == 2) {
            var k = i
            while (k + 1 < sorted.size && sorted[k + 1] - sorted[k] == 2) {
                k++
            }
            val suffix = if (sorted[i] % 2 != 0) singleLabel else doubleLabel
            result.add("${sorted[i]}-${sorted[k]}($suffix)")
            i = k + 1
        }
        // 识别连续区间
        else if (i + 1 < sorted.size && sorted[i + 1] == sorted[i] + 1) {
            val start = sorted[i]
            var k = i
            while (k + 1 < sorted.size && sorted[k + 1] == sorted[k] + 1) {
                k++
            }
            result.add("${start}-${sorted[k]}")
            i = k + 1
        }
        // 孤立的周次
        else {
            result.add("${sorted[i]}")
            i++
        }
    }
    return result.joinToString(", ")
}