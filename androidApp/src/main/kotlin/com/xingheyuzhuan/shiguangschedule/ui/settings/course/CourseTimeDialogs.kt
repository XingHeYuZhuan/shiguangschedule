package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 节次时间选择底部弹窗 (节次模式)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTimePickerBottomSheet(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    startSection: Int,
    onStartSectionChange: (Int) -> Unit,
    endSection: Int,
    onEndSectionChange: (Int) -> Unit,
    timeSlots: List<TimeSlot>,
    onDismissRequest: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempSelectedDay by remember { mutableIntStateOf(selectedDay) }
    var tempStartSection by remember { mutableIntStateOf(startSection) }
    var tempEndSection by remember { mutableIntStateOf(endSection) }

    val context = LocalContext.current
    val timeInvalidText = stringResource(R.string.toast_time_invalid)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalBottomSheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.title_select_time),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 星期
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.label_day_of_week), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    DayPicker(selectedDay = tempSelectedDay, onDaySelected = { tempSelectedDay = it })
                }
                // 开始节次
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.label_start_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionPicker(selectedSection = tempStartSection, onSectionSelected = {
                        tempStartSection = it
                        if (it > tempEndSection) tempEndSection = it
                    }, timeSlots = timeSlots)
                }
                // 结束节次
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.label_end_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionPicker(selectedSection = tempEndSection, onSectionSelected = { tempEndSection = it }, timeSlots = timeSlots)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (tempStartSection > tempEndSection) {
                        Toast.makeText(context, timeInvalidText, Toast.LENGTH_SHORT).show()
                    } else {
                        onDaySelected(tempSelectedDay)
                        onStartSectionChange(tempStartSection)
                        onEndSectionChange(tempEndSection)
                        onDismissRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        }
    }
}

/**
 * 自定义时间模式：4滚轮时间范围选择底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimeRangePickerBottomSheet(
    initialStartTime: String,
    initialEndTime: String,
    onDismissRequest: () -> Unit,
    onTimeRangeSelected: (startTime: String, endTime: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val confirmText = stringResource(R.string.action_confirm)
    val titleText = stringResource(R.string.label_custom_time)
    val startTimeLabel = stringResource(R.string.label_start_time)
    val endTimeLabel = stringResource(R.string.label_end_time)
    val endTimeInvalidText = stringResource(R.string.toast_end_time_must_be_later)

    fun parse(t: String) = t.split(":").let {
        (it.getOrNull(0)?.toIntOrNull() ?: 8) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    val (startH, startM) = parse(initialStartTime)
    val (endH, endM) = parse(initialEndTime)

    var sH by remember { mutableIntStateOf(startH) }
    var sM by remember { mutableIntStateOf(startM) }
    var eH by remember { mutableIntStateOf(endH) }
    var eM by remember { mutableIntStateOf(endM) }

    val hours = remember { (0..23).map { String.format(Locale.US, "%02d", it) } }
    val minutes = remember { (0..59).map { String.format(Locale.US, "%02d", it) } }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = startTimeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
                Text(
                    text = endTimeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 开始时间滚轮组
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NativeNumberPicker(
                        values = hours,
                        // 将 Int 转换为 "08" 这样的 String 来匹配数据源
                        selectedValue = String.format(Locale.US, "%02d", sH),
                        onValueChange = { sH = it.toInt() }, // 拿到 "08" 转回 8 存入 sH
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", style = MaterialTheme.typography.titleMedium)
                    NativeNumberPicker(
                        values = minutes,
                        selectedValue = String.format(Locale.US, "%02d", sM),
                        onValueChange = { sM = it.toInt() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("-", modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium)

                // 结束时间滚轮组
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NativeNumberPicker(
                        values = hours,
                        selectedValue = String.format(Locale.US, "%02d", eH),
                        onValueChange = { eH = it.toInt() },
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", style = MaterialTheme.typography.titleMedium)
                    NativeNumberPicker(
                        values = minutes,
                        selectedValue = String.format(Locale.US, "%02d", eM),
                        onValueChange = { eM = it.toInt() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 底部确认按钮
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val startTotal = sH * 60 + sM
                    val endTotal = eH * 60 + eM

                    if (startTotal >= endTotal) {
                        Toast.makeText(context, endTimeInvalidText, Toast.LENGTH_SHORT).show()
                    } else {
                        val startStr = String.format(Locale.US, "%02d:%02d", sH, sM)
                        val endStr = String.format(Locale.US, "%02d:%02d", eH, eM)
                        onTimeRangeSelected(startStr, endStr)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                }
            ) {
                Text(confirmText)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 星期选择对话框
 */
@Composable
fun DayPickerDialog(
    selectedDay: Int,
    onDismissRequest: () -> Unit,
    onDaySelected: (Int) -> Unit
) {
    var tempSelectedDay by remember { mutableIntStateOf(selectedDay) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.label_day_of_week), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                DayPicker(selectedDay = tempSelectedDay, onDaySelected = { tempSelectedDay = it })
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.action_cancel)) }
                    Button(onClick = {
                        onDaySelected(tempSelectedDay)
                        onDismissRequest()
                    }) { Text(stringResource(R.string.action_confirm)) }
                }
            }
        }
    }
}

@Composable
fun DayPicker(selectedDay: Int, onDaySelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val days = stringArrayResource(R.array.week_days_full_names)
    val selectedDayName = days.getOrNull(selectedDay - 1) ?: days.first()
    NativeNumberPicker(
        values = days.toList(),
        selectedValue = selectedDayName,
        onValueChange = { onDaySelected(days.indexOf(it) + 1) },
        modifier = modifier
    )
}

@Composable
fun SectionPicker(
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit,
    timeSlots: List<TimeSlot>,
    modifier: Modifier = Modifier
) {
    val sortedSlots = remember(timeSlots) {
        timeSlots.sortedBy { it.number }
    }
    val displayValues = sortedSlots.map { it.alias ?: it.number.toString() }
    val currentSlot = sortedSlots.find { it.number == selectedSection } ?: sortedSlots.firstOrNull()
    val selectedValueText = currentSlot?.let { it.alias ?: it.number.toString() } ?: ""

    NativeNumberPicker(
        values = displayValues,
        selectedValue = selectedValueText,
        onValueChange = { newValueText ->
            val pickedSlot = sortedSlots.find { (it.alias ?: it.number.toString()) == newValueText }
            pickedSlot?.let { onSectionSelected(it.number) }
        },
        modifier = modifier
    )
}