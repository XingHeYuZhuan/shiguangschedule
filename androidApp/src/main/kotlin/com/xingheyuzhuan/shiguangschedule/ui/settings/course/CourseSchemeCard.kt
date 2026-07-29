package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor

/**
 * 课程方案卡片
 * 负责展示单个课程的时间、地点、老师等具体安排
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSchemeCard(
    scheme: CourseScheme,
    courseColorMaps: List<DualColor>,
    timeSlots: List<TimeSlot>,
    onTeacherChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onColorClick: () -> Unit,
    onTimeClick: () -> Unit,
    onWeeksClick: () -> Unit,
    onDayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onToggleCustomTime: (Boolean) -> Unit,
    showRemoveButton: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            // 左侧颜色指示器块
            ColorIndicatorSection(
                colorIndex = scheme.colorIndex,
                colorMaps = courseColorMaps,
                onClick = onColorClick
            )

            // 右侧内容区
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    TextField(
                        value = scheme.teacher,
                        onValueChange = onTeacherChange,
                        placeholder = { Text(stringResource(R.string.label_teacher)) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(stringResource(R.string.label_custom_time), style = MaterialTheme.typography.labelSmall)
                    Switch(
                        checked = scheme.isCustomTime,
                        onCheckedChange = onToggleCustomTime,
                        modifier = Modifier.scale(0.7f)
                    )

                    if (showRemoveButton) {
                        IconButton(onClick = onRemoveClick) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // 地点输入
                OutlinedTextField(
                    value = scheme.position,
                    onValueChange = onPositionChange,
                    placeholder = { Text(stringResource(R.string.label_position)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 备注输入
                OutlinedTextField(
                    value = scheme.remark,
                    onValueChange = onRemarkChange,
                    placeholder = { Text(stringResource(R.string.label_remark)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.StickyNote2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    minLines = 1,
                    maxLines = 5,
                    supportingText = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${scheme.remark.length} / 300",
                                modifier = Modifier.align(Alignment.CenterEnd),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 底部区域：时间与周次（此时周次内容多会撑开 Row 的高度，进而拉伸左侧颜色条）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 时间显示逻辑
                    if (scheme.isCustomTime) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val dayNames = stringArrayResource(R.array.week_days_full_names)
                            TimeSection(
                                dayName = stringResource(R.string.label_day_of_week),
                                timeDesc = dayNames.getOrNull(scheme.day - 1) ?: "",
                                onClick = onDayClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TimeSection(
                                dayName = stringResource(R.string.label_custom_time),
                                timeDesc = "${scheme.customStartTime.ifBlank { "00:00" }}-${scheme.customEndTime.ifBlank { "00:00" }}",
                                onClick = onTimeClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        val dayNames = stringArrayResource(R.array.week_days_full_names)
                        val startAlias = timeSlots.find { it.number == scheme.startSection }?.alias
                        val endAlias = timeSlots.find { it.number == scheme.endSection }?.alias
                        val startDisplay = startAlias ?: scheme.startSection.toString()
                        val endDisplay = endAlias ?: scheme.endSection.toString()

                        val sectionRangeSuffix = stringResource(R.string.label_section_range_suffix)
                        val timeDesc = if (startDisplay == endDisplay) {
                            "$startDisplay $sectionRangeSuffix"
                        } else {
                            "$startDisplay-$endDisplay $sectionRangeSuffix"
                        }

                        TimeSection(
                            dayName = dayNames.getOrNull(scheme.day - 1) ?: "",
                            timeDesc = timeDesc,
                            onClick = onTimeClick,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    // 周次展示（展示完整周次，撑起整个卡片高度）
                    WeekSection(
                        selectedWeeks = scheme.weeks,
                        onClick = onWeeksClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}