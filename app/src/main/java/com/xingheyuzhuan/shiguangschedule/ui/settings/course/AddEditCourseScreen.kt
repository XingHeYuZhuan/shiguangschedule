package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.xingheyuzhuan.shiguangschedule.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCourseScreen(
    courseId: String?,
    onNavigateBack: () -> Unit,
    initialDay: Int?,
    initialSection: Int?
) {
    val viewModel: AddEditCourseViewModel = viewModel(
        factory = AddEditCourseViewModel.Factory(
            courseId = courseId,
            initialDay = initialDay,
            initialSection = initialSection
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWeekSelectorDialog by remember { mutableStateOf(false) }
    var showColorSelectorDialog by remember { mutableStateOf(false) }
    var showCourseTimeDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val saveSuccessText = stringResource(R.string.toast_save_success)
    val deleteSuccessText = stringResource(R.string.toast_delete_success)
    val nameEmptyText = stringResource(R.string.toast_name_empty)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                UiEvent.SaveSuccess -> {
                    Toast.makeText(context, saveSuccessText, Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                UiEvent.DeleteSuccess -> {
                    Toast.makeText(context, deleteSuccessText, Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                UiEvent.Cancel -> {
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) {
                            stringResource(R.string.title_edit_course)
                        } else {
                            stringResource(R.string.title_add_course)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (uiState.name.isBlank()) {
                                Toast.makeText(context, nameEmptyText, Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.onSave()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(R.string.a11y_save))
                    }
                    if (uiState.isEditing) {
                        IconButton(onClick = viewModel::onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.a11y_delete))
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.label_course_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.teacher,
                onValueChange = viewModel::onTeacherChange,
                label = { Text(stringResource(R.string.label_teacher)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.position,
                onValueChange = viewModel::onPositionChange,
                label = { Text(stringResource(R.string.label_position)) },
                modifier = Modifier.fillMaxWidth()
            )

            CourseTimePickerButton(
                day = uiState.day,
                startSection = uiState.startSection,
                endSection = uiState.endSection,
                timeSlots = uiState.timeSlots,
                onButtonClick = { showCourseTimeDialog = true }
            )

            WeekSelector(
                selectedWeeks = uiState.weeks,
                onWeekClick = { showWeekSelectorDialog = true }
            )

            ColorPicker(
                selectedColor = uiState.color,
                onColorClick = { showColorSelectorDialog = true }
            )
        }
    }

    if (showWeekSelectorDialog) {
        WeekSelectorBottomSheet(
            totalWeeks = uiState.semesterTotalWeeks,
            selectedWeeks = uiState.weeks,
            onDismissRequest = { showWeekSelectorDialog = false },
            onConfirm = { newWeeks ->
                viewModel.onWeeksChange(newWeeks)
                showWeekSelectorDialog = false
            }
        )
    }

    if (showColorSelectorDialog) {
        ColorPickerBottomSheet(
            colors = uiState.courseColors,
            selectedColor = uiState.color,
            onDismissRequest = { showColorSelectorDialog = false },
            onConfirm = { newColor ->
                viewModel.onColorChange(newColor)
                showColorSelectorDialog = false
            }
        )
    }

    if (showCourseTimeDialog) {
        CourseTimePickerBottomSheet(
            selectedDay = uiState.day,
            onDaySelected = viewModel::onDayChange,
            startSection = uiState.startSection,
            onStartSectionChange = viewModel::onStartSectionChange,
            endSection = uiState.endSection,
            onEndSectionChange = viewModel::onEndSectionChange,
            timeSlots = uiState.timeSlots,
            onDismissRequest = { showCourseTimeDialog = false }
        )
    }
}

@Composable
private fun CourseTimePickerButton(
    day: Int,
    startSection: Int,
    endSection: Int,
    timeSlots: List<TimeSlot>,
    onButtonClick: () -> Unit
) {
    val days = stringArrayResource(R.array.week_days_full_names)
    val dayName = days.getOrNull(day - 1) ?: days.first()

    val sectionCount = if (endSection > startSection) {
        stringResource(R.string.course_time_sections_count, endSection - startSection + 1)
    } else {
        ""
    }

    val sectionsText = if (timeSlots.isNotEmpty()) {
        "${startSection}-${endSection}${stringResource(R.string.label_section_range_suffix)}"
    } else {
        stringResource(R.string.label_none)
    }

    val buttonText = "$dayName $sectionsText $sectionCount"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.label_course_time), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onButtonClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(text = buttonText, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseTimePickerBottomSheet(
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

    var tempSelectedDay by remember { mutableStateOf(selectedDay) }
    var tempStartSection by remember { mutableStateOf(startSection) }
    var tempEndSection by remember { mutableStateOf(endSection) }

    val context = LocalContext.current
    val timeInvalidText = stringResource(R.string.toast_time_invalid)
    val confirmText = stringResource(R.string.action_confirm)

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(R.string.label_day_of_week), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    DayPicker(
                        selectedDay = tempSelectedDay,
                        onDaySelected = { newDay -> tempSelectedDay = newDay }
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(R.string.label_start_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionPicker(
                        selectedSection = tempStartSection,
                        onSectionSelected = { newStart ->
                            tempStartSection = newStart
                        },
                        timeSlots = timeSlots
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(R.string.label_end_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionPicker(
                        selectedSection = tempEndSection,
                        onSectionSelected = { newEnd ->
                            tempEndSection = newEnd
                        },
                        timeSlots = timeSlots
                    )
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
                        onDismissRequest() // 关闭弹窗
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(confirmText, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun DayPicker(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = stringArrayResource(R.array.week_days_full_names)
    val selectedDayName = days.getOrNull(selectedDay - 1) ?: days.first()

    NativeNumberPicker(
        values = days.toList(),
        selectedValue = selectedDayName,
        onValueChange = { dayName ->
            val dayNumber = days.indexOf(dayName) + 1
            onDaySelected(dayNumber)
        },
        modifier = modifier
    )
}

@Composable
private fun SectionPicker(
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit,
    timeSlots: List<TimeSlot>,
    modifier: Modifier = Modifier,
) {
    val sectionNumbers = timeSlots.map { it.number }

    NativeNumberPicker(
        values = sectionNumbers,
        selectedValue = selectedSection,
        onValueChange = onSectionSelected,
        modifier = modifier
    )
}

@Composable
private fun WeekSelector(
    selectedWeeks: Set<Int>,
    onWeekClick: () -> Unit
) {
    val labelCourseWeeks = stringResource(R.string.label_course_weeks)
    val buttonSelectWeeks = stringResource(R.string.button_select_weeks)
    val textWeeksSelected = stringResource(R.string.text_weeks_selected)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = labelCourseWeeks, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onWeekClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            val weeksText = if (selectedWeeks.isEmpty()) {
                buttonSelectWeeks
            } else {
                String.format(textWeeksSelected, selectedWeeks.sorted().joinToString(", "))
            }
            Text(text = weeksText, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WeekSelectorBottomSheet(
    totalWeeks: Int,
    selectedWeeks: Set<Int>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelectedWeeks by remember { mutableStateOf(selectedWeeks) }

    val titleSelectWeeks = stringResource(R.string.title_select_weeks)
    val actionSelectAll = stringResource(R.string.action_select_all)
    val actionSingleWeek = stringResource(R.string.action_single_week)
    val actionDoubleWeek = stringResource(R.string.action_double_week)
    val actionCancel = stringResource(R.string.action_cancel)
    val actionConfirm = stringResource(R.string.action_confirm)

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
                text = titleSelectWeeks,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(totalWeeks) { week ->
                    val weekNumber = week + 1
                    val isSelected = tempSelectedWeeks.contains(weekNumber)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                tempSelectedWeeks = if (isSelected) {
                                    tempSelectedWeeks - weekNumber
                                } else {
                                    tempSelectedWeeks + weekNumber
                                }
                            }
                            .then(
                                if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = weekNumber.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InputChip(
                    selected = tempSelectedWeeks.size == totalWeeks,
                    onClick = {
                        tempSelectedWeeks = if (tempSelectedWeeks.size == totalWeeks) {
                            emptySet()
                        } else {
                            (1..totalWeeks).toSet()
                        }
                    },
                    label = { Text(actionSelectAll) }
                )
                InputChip(
                    selected = tempSelectedWeeks.all { it % 2 != 0 } && tempSelectedWeeks.isNotEmpty(),
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 != 0 }.toSet()
                    },
                    label = { Text(actionSingleWeek) }
                )
                InputChip(
                    selected = tempSelectedWeeks.all { it % 2 == 0 } && tempSelectedWeeks.isNotEmpty(),
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet()
                    },
                    label = { Text(actionDoubleWeek) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(actionCancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        onConfirm(tempSelectedWeeks)
                        coroutineScope.launch { modalBottomSheetState.hide() }.invokeOnCompletion {
                            if (!modalBottomSheetState.isVisible) {
                                onDismissRequest()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(actionConfirm, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(
    selectedColor: Color,
    onColorClick: () -> Unit
) {
    val labelCourseColor = stringResource(R.string.label_course_color)
    val buttonSelectColor = stringResource(R.string.button_select_color)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = labelCourseColor, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onColorClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = selectedColor)
        ) {
            Text(buttonSelectColor, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerBottomSheet(
    colors: List<Color>,
    selectedColor: Color,
    onDismissRequest: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelectedColor by remember { mutableStateOf(selectedColor) }

    val titleSelectColor = stringResource(R.string.title_select_color)
    val actionCancel = stringResource(R.string.action_cancel)
    val actionConfirm = stringResource(R.string.action_confirm)
    val a11yColorSelected = stringResource(R.string.a11y_color_selected)

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
                text = titleSelectColor,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(colors) { color ->
                    val isSelected = tempSelectedColor == color
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                tempSelectedColor = color
                            }
                            .then(
                                if (isSelected) Modifier.padding(4.dp)
                                    .clip(CircleShape) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(color)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = a11yColorSelected,
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(actionCancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        onConfirm(tempSelectedColor)
                        coroutineScope.launch { modalBottomSheetState.hide() }.invokeOnCompletion {
                            if (!modalBottomSheetState.isVisible) {
                                onDismissRequest()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(actionConfirm, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}