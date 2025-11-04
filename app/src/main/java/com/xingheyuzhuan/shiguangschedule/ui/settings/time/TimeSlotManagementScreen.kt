package com.xingheyuzhuan.shiguangschedule.ui.settings.time

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 时间段管理界面的 Compose UI。
 *
 * @param onBackClick 返回上一页的回调。
 * @param timeSlotViewModel ViewModel，负责管理 UI 状态和业务逻辑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotManagementScreen(
    onBackClick: () -> Unit,
    timeSlotViewModel: TimeSlotViewModel = viewModel(factory = TimeSlotViewModelFactory)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by timeSlotViewModel.timeSlotsUiState.collectAsState()

    val localTimeSlots = remember {
        mutableStateListOf<TimeSlot>().apply { addAll(uiState.timeSlots.sortedBy { it.number }) }
    }

    var localDefaultClassDuration by remember { mutableStateOf(uiState.defaultClassDuration) }
    var localDefaultBreakDuration by remember { mutableStateOf(uiState.defaultBreakDuration) }

    val titleTimeSlotManagement = stringResource(R.string.title_time_slot_management)
    val a11yBack = stringResource(R.string.a11y_back)
    val a11yAddTimeSlot = stringResource(R.string.a11y_add_time_slot)
    val a11ySaveAllSettings = stringResource(R.string.a11y_save_all_settings)
    val toastSettingsSaved = stringResource(R.string.toast_settings_saved)
    val toastSlotRemovedUnsaved = stringResource(R.string.toast_slot_removed_unsaved)
    val textNoTimeSlotsHint = stringResource(R.string.text_no_time_slots_hint)
    val toastSlotModifiedUnsaved = stringResource(R.string.toast_slot_modified_unsaved)
    val toastSlotAddedUnsaved = stringResource(R.string.toast_slot_added_unsaved)


    LaunchedEffect(uiState) {
        localTimeSlots.clear()
        localTimeSlots.addAll(uiState.timeSlots.sortedBy { it.number })
        localDefaultClassDuration = uiState.defaultClassDuration
        localDefaultBreakDuration = uiState.defaultBreakDuration
    }

    var showEditBottomSheet by remember { mutableStateOf(false) }
    var editingTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(titleTimeSlotManagement) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = a11yBack)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingTimeSlot = null
                        editingIndex = null
                        showEditBottomSheet = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = a11yAddTimeSlot)
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val sortedAndNumberedSlots = localTimeSlots
                                .sortedBy { it.startTime.let { timeStr -> try { LocalTime.parse(timeStr) } catch (e: DateTimeParseException) { LocalTime.MAX } } }
                                .mapIndexed { index, slot -> slot.copy(number = index + 1) }

                            timeSlotViewModel.onSaveAllSettings(
                                timeSlots = sortedAndNumberedSlots,
                                classDuration = localDefaultClassDuration,
                                breakDuration = localDefaultBreakDuration
                            )
                            Toast.makeText(context, toastSettingsSaved, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = a11ySaveAllSettings)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                HorizontalDivider()
                DefaultDurationSettings(
                    defaultClassDuration = localDefaultClassDuration,
                    onClassDurationChange = { newValue ->
                        localDefaultClassDuration = newValue
                    },
                    defaultBreakDuration = localDefaultBreakDuration,
                    onBreakDurationChange = { newValue ->
                        localDefaultBreakDuration = newValue
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                if (localTimeSlots.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(textNoTimeSlotsHint)
                    }
                }
            }

            itemsIndexed(localTimeSlots, key = { _, slot -> "${slot.number}-${slot.startTime}" }) { index, timeSlot ->
                TimeSlotItem(
                    timeSlot = timeSlot,
                    onEditClick = {
                        editingTimeSlot = timeSlot
                        editingIndex = index
                        showEditBottomSheet = true
                    },
                    onDeleteClick = {
                        localTimeSlots.removeAt(index)
                        val renumberedList = localTimeSlots
                            .sortedBy { it.startTime.let { timeStr -> try { LocalTime.parse(timeStr) } catch (e: DateTimeParseException) { LocalTime.MAX } } }
                            .mapIndexed { i, slot ->
                                slot.copy(number = i + 1)
                            }
                        localTimeSlots.clear()
                        localTimeSlots.addAll(renumberedList)
                        Toast.makeText(context, toastSlotRemovedUnsaved, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        if (showEditBottomSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val isEditing = editingTimeSlot != null

            val calculatedInitialStartTime: String
            val calculatedInitialEndTime: String

            if (isEditing) {
                calculatedInitialStartTime = editingTimeSlot!!.startTime
                calculatedInitialEndTime = editingTimeSlot!!.endTime
            } else {
                if (localTimeSlots.isNotEmpty()) {
                    val lastTimeSlot = localTimeSlots.maxByOrNull {
                        it.startTime.let { timeStr ->
                            try {
                                LocalTime.parse(timeStr)
                            } catch (e: DateTimeParseException) {
                                LocalTime.MAX
                            }
                        }
                    }!!
                    val lastEndTime = try { LocalTime.parse(lastTimeSlot.endTime, DateTimeFormatter.ofPattern("HH:mm")) } catch (e: DateTimeParseException) { LocalTime.MIDNIGHT }

                    val newStartTime = lastEndTime.plusMinutes(localDefaultBreakDuration.toLong())
                    val newEndTime = newStartTime.plusMinutes(localDefaultClassDuration.toLong())

                    calculatedInitialStartTime = newStartTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    calculatedInitialEndTime = newEndTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                } else {
                    val defaultStart = LocalTime.of(8, 0)
                    val defaultEnd = defaultStart.plusMinutes(localDefaultClassDuration.toLong())
                    calculatedInitialStartTime = defaultStart.format(DateTimeFormatter.ofPattern("HH:mm"))
                    calculatedInitialEndTime = defaultEnd.format(DateTimeFormatter.ofPattern("HH:mm"))
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showEditBottomSheet = false },
                sheetState = sheetState
            ) {
                TimeSlotEditContent(
                    initialNumber = editingTimeSlot?.number ?: (localTimeSlots.maxOfOrNull { it.number }?.plus(1) ?: 1),
                    initialStartTime = calculatedInitialStartTime,
                    initialEndTime = calculatedInitialEndTime,
                    isEditing = isEditing,
                    onDismiss = { showEditBottomSheet = false },
                    onConfirm = { number, startTime, endTime ->
                        val newOrUpdatedSlot = TimeSlot(number, startTime, endTime, courseTableId = "")
                        if (isEditing && editingIndex != null) {
                            localTimeSlots[editingIndex!!] = newOrUpdatedSlot
                            Toast.makeText(context, toastSlotModifiedUnsaved, Toast.LENGTH_SHORT).show()
                        } else {
                            localTimeSlots.add(newOrUpdatedSlot)
                            Toast.makeText(context, toastSlotAddedUnsaved, Toast.LENGTH_SHORT).show()
                        }
                        val renumberedAndSortedList = localTimeSlots
                            .sortedBy { it.startTime.let { timeStr -> try { LocalTime.parse(timeStr) } catch (e: DateTimeParseException) { LocalTime.MAX } } }
                            .mapIndexed { i, slot -> slot.copy(number = i + 1) }
                        localTimeSlots.clear()
                        localTimeSlots.addAll(renumberedAndSortedList)

                        showEditBottomSheet = false
                    }
                )
            }
        }
    }
}

/**
 * 默认时长设置的 UI 组件
 */
@Composable
fun DefaultDurationSettings(
    defaultClassDuration: Int,
    onClassDurationChange: (Int) -> Unit,
    defaultBreakDuration: Int,
    onBreakDurationChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val titleDefaultDurationSettings = stringResource(R.string.title_default_duration_settings)
    val labelClassDuration = stringResource(R.string.label_class_duration_minutes)
    val toastClassDurationPositive = stringResource(R.string.toast_class_duration_positive)
    val labelBreakDuration = stringResource(R.string.label_break_duration_minutes)
    val toastBreakDurationNonNegative = stringResource(R.string.toast_break_duration_non_negative)

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(titleDefaultDurationSettings, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = if (defaultClassDuration == 0) "" else defaultClassDuration.toString(),
                onValueChange = { newValueStr ->
                    val newIntValue = newValueStr.toIntOrNull()
                    if (newValueStr.isEmpty()) {
                        onClassDurationChange(0)
                    } else if (newIntValue != null && newIntValue > 0) {
                        onClassDurationChange(newIntValue)
                    } else if (newIntValue != null){
                        Toast.makeText(context, toastClassDurationPositive, Toast.LENGTH_SHORT).show()
                    }
                },
                label = { Text(labelClassDuration) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = if (defaultBreakDuration == -1) "" else defaultBreakDuration.toString(),
                onValueChange = { newValueStr ->
                    val newIntValue = newValueStr.toIntOrNull()
                    if (newValueStr.isEmpty()) {
                        onBreakDurationChange(-1)
                    } else if (newIntValue != null && newIntValue >= 0) {
                        onBreakDurationChange(newIntValue)
                    } else if (newIntValue != null) {
                        Toast.makeText(context, toastBreakDurationNonNegative, Toast.LENGTH_SHORT).show()
                    }
                },
                label = { Text(labelBreakDuration) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
}


/**
 * 单个时间段列表项的 UI 组件
 */
@Composable
fun TimeSlotItem(
    timeSlot: TimeSlot,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val timeSlotSectionNumber = stringResource(R.string.time_slot_section_number)
    val a11yDeleteTimeSlot = stringResource(R.string.a11y_delete_time_slot)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEditClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = timeSlotSectionNumber.format(timeSlot.number),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${timeSlot.startTime} - ${timeSlot.endTime}",
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = a11yDeleteTimeSlot)
            }
        }
    }
}

/**
 * 编辑/添加时间段的底部弹窗内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotEditContent(
    initialNumber: Int,
    initialStartTime: String,
    initialEndTime: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (number: Int, startTime: String, endTime: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val (initialStartHour, initialStartMinute) = parseTimeString(initialStartTime)
    val (initialEndHour, initialEndMinute) = parseTimeString(initialEndTime)

    var startHourState by remember { mutableStateOf(initialStartHour) }
    var startMinuteState by remember { mutableStateOf(initialStartMinute) }
    var endHourState by remember { mutableStateOf(initialEndHour) }
    var endMinuteState by remember { mutableStateOf(initialEndMinute) }

    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    val dialogTitleEdit = stringResource(R.string.dialog_title_edit_time_slot)
    val dialogTitleAdd = stringResource(R.string.dialog_title_add_time_slot)
    val labelStart = stringResource(R.string.label_time_picker_start)
    val labelEnd = stringResource(R.string.label_time_picker_end)
    val labelHour = stringResource(R.string.label_time_picker_hour)
    val labelMinute = stringResource(R.string.label_time_picker_minute)
    val actionCancel = stringResource(R.string.action_cancel)
    val actionSaveChanges = stringResource(R.string.action_save_changes)
    val actionAdd = stringResource(R.string.action_add)
    val toastEndTimeMustBeLater = stringResource(R.string.toast_end_time_must_be_later)

    val currentTimeRange by remember {
        derivedStateOf {
            val start = LocalTime.of(startHourState, startMinuteState)
            val end = LocalTime.of(endHourState, endMinuteState)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            "${start.format(formatter)} - ${end.format(formatter)}"
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

        Spacer(modifier = Modifier.height(16.dp))

        // 时间选择器行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(labelStart, style = MaterialTheme.typography.bodySmall)
                Text(labelHour, style = MaterialTheme.typography.labelSmall)
                NativeNumberPicker(
                    values = hours,
                    selectedValue = startHourState,
                    onValueChange = { startHourState = it },
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.width(4.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("", style = MaterialTheme.typography.bodySmall)
                Text(labelMinute, style = MaterialTheme.typography.labelSmall)
                NativeNumberPicker(
                    values = minutes,
                    selectedValue = startMinuteState,
                    onValueChange = { startMinuteState = it },
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(labelEnd, style = MaterialTheme.typography.bodySmall)
                Text(labelHour, style = MaterialTheme.typography.labelSmall)
                NativeNumberPicker(
                    values = hours,
                    selectedValue = endHourState,
                    onValueChange = { endHourState = it },
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.width(4.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("", style = MaterialTheme.typography.bodySmall)
                Text(labelMinute, style = MaterialTheme.typography.labelSmall)
                NativeNumberPicker(
                    values = minutes,
                    selectedValue = endMinuteState,
                    onValueChange = { endMinuteState = it },
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
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
                    TextButton(onClick = onDismiss) {
                        Text(actionCancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val startTotalMinutes = startHourState * 60 + startMinuteState
                        val endTotalMinutes = endHourState * 60 + endMinuteState
                        if (endTotalMinutes > startTotalMinutes) {
                            val formatter = DateTimeFormatter.ofPattern("HH:mm")
                            val startTime = LocalTime.of(startHourState, startMinuteState).format(formatter)
                            val endTime = LocalTime.of(endHourState, endMinuteState).format(formatter)
                            onConfirm(initialNumber, startTime, endTime)
                        } else {
                            coroutineScope.launch {
                                Toast.makeText(context, toastEndTimeMustBeLater, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(if (isEditing) actionSaveChanges else actionAdd)
                    }
                }
            }
        }
    }
}


/**
 * 解析时间字符串，返回小时和分钟。
 * @param timeString 格式为 "HH:mm" 的时间字符串。
 * @return 包含小时和分钟的 Pair。如果解析失败，返回 (0, 0)。
 */
fun parseTimeString(timeString: String): Pair<Int, Int> {
    return try {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val localTime = LocalTime.parse(timeString, formatter)
        Pair(localTime.hour, localTime.minute)
    } catch (e: DateTimeParseException) {
        Log.e("TimeSlotEditContent", "Error parsing time string: $timeString", e)
        Pair(0, 0)
    }
}