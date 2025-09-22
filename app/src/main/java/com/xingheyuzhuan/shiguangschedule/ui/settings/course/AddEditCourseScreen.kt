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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import kotlinx.coroutines.launch

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

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                UiEvent.SaveSuccess -> {
                    Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                UiEvent.DeleteSuccess -> {
                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
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
                title = { Text(text = if (uiState.isEditing) "编辑课程" else "添加课程") },
                navigationIcon = {
                    IconButton(onClick = viewModel::onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (uiState.name.isBlank()) {
                                Toast.makeText(context, "课程名称不能为空", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.onSave()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "保存")
                    }
                    if (uiState.isEditing) {
                        IconButton(onClick = viewModel::onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
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
                label = { Text("课程名称*") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.teacher,
                onValueChange = viewModel::onTeacherChange,
                label = { Text("教师") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.position,
                onValueChange = viewModel::onPositionChange,
                label = { Text("地点") },
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
    val dayName = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").getOrNull(day - 1) ?: "周一"
    val sectionCount = if (endSection > startSection) "(${endSection - startSection + 1}节)" else ""
    val sectionsText = if (timeSlots.isNotEmpty()) {
        "${startSection}-${endSection}节"
    } else {
        "无"
    }
    val buttonText = "$dayName $sectionsText $sectionCount"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "上课时间", style = MaterialTheme.typography.titleMedium)
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
                text = "选择上课时间",
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
                    Text(text = "星期", style = MaterialTheme.typography.titleSmall)
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
                    Text(text = "开始节次", style = MaterialTheme.typography.titleSmall)
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
                    Text(text = "结束节次", style = MaterialTheme.typography.titleSmall)
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
                        Toast.makeText(context, "开始节次不能大于结束节次", Toast.LENGTH_SHORT).show()
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
                Text("确定", color = MaterialTheme.colorScheme.onPrimary)
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
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val selectedDayName = days.getOrNull(selectedDay - 1) ?: "周一"

    NativeNumberPicker(
        values = days,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "上课周次", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onWeekClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            val weeksText = if (selectedWeeks.isEmpty()) {
                "选择周数"
            } else {
                "已选: ${selectedWeeks.sorted().joinToString(", ")}"
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
                text = "选择上课周次",
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
                    label = { Text("全选") }
                )
                InputChip(
                    selected = tempSelectedWeeks.all { it % 2 != 0 } && tempSelectedWeeks.isNotEmpty(),
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 != 0 }.toSet()
                    },
                    label = { Text("单周") }
                )
                InputChip(
                    selected = tempSelectedWeeks.all { it % 2 == 0 } && tempSelectedWeeks.isNotEmpty(),
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet()
                    },
                    label = { Text("双周") }
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
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("确定", color = MaterialTheme.colorScheme.onPrimary)
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "课程颜色", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onColorClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = selectedColor)
        ) {
            Text("选择颜色", color = Color.White)
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
                text = "选择课程颜色",
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
                                contentDescription = "已选择",
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
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("确定", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}