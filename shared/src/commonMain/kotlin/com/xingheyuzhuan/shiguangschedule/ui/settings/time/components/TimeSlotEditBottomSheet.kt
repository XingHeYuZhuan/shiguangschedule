package com.xingheyuzhuan.shiguangschedule.ui.settings.time.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_add
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_save_changes
import shiguangschedule.shared.generated.resources.dialog_title_add_time_slot
import shiguangschedule.shared.generated.resources.dialog_title_edit_time_slot
import shiguangschedule.shared.generated.resources.label_time_picker_end
import shiguangschedule.shared.generated.resources.label_time_picker_hour
import shiguangschedule.shared.generated.resources.label_time_picker_minute
import shiguangschedule.shared.generated.resources.label_time_picker_start
import shiguangschedule.shared.generated.resources.label_time_slot_alias
import shiguangschedule.shared.generated.resources.toast_end_time_must_be_later
import shiguangschedule.shared.generated.resources.toast_time_conflict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotEditContent(
    existingTimeSlots: List<TimeSlot>,
    initialNumber: Int,
    initialStartTime: String,
    initialEndTime: String,
    initialAlias: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (number: Int, startTime: String, endTime: String, alias: String?) -> Unit
) {
    val (initialStartHour, initialStartMinute) = parseTimeString(initialStartTime)
    val (initialEndHour, initialEndMinute) = parseTimeString(initialEndTime)

    var startHourState by remember { mutableIntStateOf(initialStartHour) }
    var startMinuteState by remember { mutableIntStateOf(initialStartMinute) }
    var endHourState by remember { mutableIntStateOf(initialEndHour) }
    var endMinuteState by remember { mutableIntStateOf(initialEndMinute) }
    var aliasState by remember { mutableStateOf(initialAlias ?: "") }

    val staticHours = remember { (0..23).map { formatTwoDigits(it) } }
    val staticMinutes = remember { (0..59).map { formatTwoDigits(it) } }

    val dialogTitleEdit = stringResource(Res.string.dialog_title_edit_time_slot)
    val dialogTitleAdd = stringResource(Res.string.dialog_title_add_time_slot)
    val labelStart = stringResource(Res.string.label_time_picker_start)
    val labelEnd = stringResource(Res.string.label_time_picker_end)
    val labelHour = stringResource(Res.string.label_time_picker_hour)
    val labelMinute = stringResource(Res.string.label_time_picker_minute)
    val actionCancel = stringResource(Res.string.action_cancel)
    val actionSaveChanges = stringResource(Res.string.action_save_changes)
    val actionAdd = stringResource(Res.string.action_add)
    val toastEndTimeMustBeLater = stringResource(Res.string.toast_end_time_must_be_later)
    val toastTimeConflict = stringResource(Res.string.toast_time_conflict)

    val currentTimeRange by remember(startHourState, startMinuteState, endHourState, endMinuteState) {
        derivedStateOf {
            val start = LocalTime(startHourState, startMinuteState)
            val end = LocalTime(endHourState, endMinuteState)
            "${formatTime(start)} - ${formatTime(end)}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditing) dialogTitleEdit else dialogTitleAdd,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = aliasState,
            onValueChange = { if (it.length <= 5) aliasState = it },
            label = { Text(stringResource(Res.string.label_time_slot_alias)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            supportingText = {
                Text(
                    text = "${aliasState.length}/5",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(labelStart, style = MaterialTheme.typography.bodySmall)
                    Text(labelHour, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("", style = MaterialTheme.typography.bodySmall)
                    Text(labelMinute, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(labelEnd, style = MaterialTheme.typography.bodySmall)
                    Text(labelHour, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("", style = MaterialTheme.typography.bodySmall)
                    Text(labelMinute, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NativeNumberPicker(
                    values = staticHours,
                    selectedValue = formatTwoDigits(startHourState),
                    onValueChange = { startHourState = it.toInt() },
                    modifier = Modifier
                        .height(150.dp)
                        .weight(1f)
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
                NativeNumberPicker(
                    values = staticMinutes,
                    selectedValue = formatTwoDigits(startMinuteState),
                    onValueChange = { startMinuteState = it.toInt() },
                    modifier = Modifier
                        .height(150.dp)
                        .weight(1f)
                )
            }

            Text(
                "-",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NativeNumberPicker(
                    values = staticHours,
                    selectedValue = formatTwoDigits(endHourState),
                    onValueChange = { endHourState = it.toInt() },
                    modifier = Modifier
                        .height(150.dp)
                        .weight(1f)
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
                NativeNumberPicker(
                    values = staticMinutes,
                    selectedValue = formatTwoDigits(endMinuteState),
                    onValueChange = { endMinuteState = it.toInt() },
                    modifier = Modifier
                        .height(150.dp)
                        .weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = currentTimeRange,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(actionCancel) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val startTimeObj = LocalTime(startHourState, startMinuteState)
                        val endTimeObj = LocalTime(endHourState, endMinuteState)

                        if (endTimeObj <= startTimeObj) {
                            ToastManager.show(toastEndTimeMustBeLater)
                            return@Button
                        }

                        val hasConflict = existingTimeSlots.any { slot ->
                            if (isEditing && slot.number == initialNumber) return@any false
                            val slotStart = parseLocalTimeSafely(slot.startTime)
                            val slotEnd = parseLocalTimeSafely(slot.endTime)
                            maxOf(startTimeObj, slotStart) < minOf(endTimeObj, slotEnd)
                        }

                        if (hasConflict) {
                            ToastManager.show(toastTimeConflict)
                            return@Button
                        }

                        onConfirm(
                            initialNumber,
                            formatTime(startTimeObj),
                            formatTime(endTimeObj),
                            aliasState.ifBlank { null }
                        )
                    }) {
                        Text(if (isEditing) actionSaveChanges else actionAdd)
                    }
                }
            }
        }
    }
}

// 供 BottomSheet 和外部使用的辅助计算工具函数

fun calculateInitialTimes(
    isEditing: Boolean,
    editingTimeSlot: TimeSlot?,
    localTimeSlots: List<TimeSlot>,
    breakDur: Int,
    classDur: Int
): Pair<String, String> {
    if (isEditing && editingTimeSlot != null) return Pair(editingTimeSlot.startTime, editingTimeSlot.endTime)

    return if (localTimeSlots.isNotEmpty()) {
        val lastEndTimeStr = localTimeSlots.maxOf { it.endTime }
        val lastEndTime = parseLocalTimeSafely(lastEndTimeStr, fallback = LocalTime(8, 0))
        val start = lastEndTime.addMinutes(breakDur)
        val end = start.addMinutes(classDur)
        Pair(formatTime(start), formatTime(end))
    } else {
        val start = LocalTime(8, 0)
        Pair(formatTime(start), formatTime(start.addMinutes(classDur)))
    }
}

fun formatTwoDigits(value: Int): String = value.toString().padStart(2, '0')

fun formatTime(time: LocalTime): String = "${formatTwoDigits(time.hour)}:${formatTwoDigits(time.minute)}"

fun parseTimeString(timeString: String): Pair<Int, Int> {
    return try {
        val time = LocalTime.parse(timeString)
        Pair(time.hour, time.minute)
    } catch (_: Exception) {
        Pair(0, 0)
    }
}

fun parseLocalTimeSafely(timeStr: String, fallback: LocalTime = LocalTime(23, 59)): LocalTime {
    return try {
        LocalTime.parse(timeStr)
    } catch (_: Exception) {
        fallback
    }
}

fun LocalTime.addMinutes(minutes: Int): LocalTime {
    val totalMinutes = this.hour * 60 + this.minute + minutes
    val newTotalMinutes = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    return LocalTime(newTotalMinutes / 60, newTotalMinutes % 60)
}