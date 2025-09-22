package com.xingheyuzhuan.shiguangschedule.ui.settings

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xingheyuzhuan.shiguangschedule.Screen
import com.xingheyuzhuan.shiguangschedule.ui.components.BottomNavigationBar
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory)
) {
    val appSettings by viewModel.appSettingsState.collectAsState()

    val scrollState = rememberScrollState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showWeekends = appSettings.showWeekends
    val semesterStartDateString = appSettings.semesterStartDate
    val semesterTotalWeeks = appSettings.semesterTotalWeeks
    val displayCurrentWeek by viewModel.currentWeekState.collectAsState()

    val semesterStartDate: LocalDate? = remember(semesterStartDateString) {
        semesterStartDateString?.let {
            try {
                LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: DateTimeParseException) {
                Log.e("SettingsScreen", "Failed to parse date string: $it", e)
                null
            }
        }
    }

    var showTotalWeeksDialog by remember { mutableStateOf(false) }
    var showManualWeekDialog by remember { mutableStateOf(false) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("课程表设置", style = MaterialTheme.typography.headlineSmall)
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("是否显示周末", style = MaterialTheme.typography.bodyMedium)
                    Text("开启后，课程表中将显示周末的课程安排", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = showWeekends,
                    onCheckedChange = { isChecked -> viewModel.onShowWeekendsChanged(isChecked) }
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePickerModal = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("开始上课时间", style = MaterialTheme.typography.bodyMedium)
                    Text("选择学期第一周的周一", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = semesterStartDate?.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) ?: "未设置",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTotalWeeksDialog = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("本学期总周数", style = MaterialTheme.typography.bodyMedium)
                    Text("设置学期总共持续多少周", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "$semesterTotalWeeks 周",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("当前周数", style = MaterialTheme.typography.bodyMedium)
                    val weekStatusText = when {
                        semesterStartDate == null -> "请设置开始日期"
                        displayCurrentWeek == null -> "假期中"
                        else -> "第 $displayCurrentWeek / $semesterTotalWeeks 周"
                    }
                    Text(weekStatusText, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        showManualWeekDialog = true
                    }
                ) {
                    Text("手动调整")
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.TimeSlotSettings.route) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("自定义时间段", style = MaterialTheme.typography.bodyMedium)
                    Text("编辑您的课程时间段设置", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Edit, contentDescription = "编辑时间段")
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.NotificationSettings.route) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("课程提醒设置", style = MaterialTheme.typography.bodyMedium)
                    Text("管理您的课程提醒通知", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Edit, contentDescription = "编辑提醒设置")
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.ManageCourseTables.route) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("管理课表", style = MaterialTheme.typography.bodyMedium)
                    Text("管理多份课程表，可切换、删除", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Edit, contentDescription = "管理课表")
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.CourseTableConversion.route) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("课表导入/导出", style = MaterialTheme.typography.bodyMedium)
                    Text("支持多种导入导出方式", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Edit, contentDescription = "课表导入/导出")
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.MoreOptions.route) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("更多", style = MaterialTheme.typography.bodyMedium)
                    Text("提供我们的更多信息", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.MoreHoriz, contentDescription = "更多选项")
            }
        }
    }

    if (showDatePickerModal) {
        DatePickerModal(
            onDateSelected = { selectedDateMillis ->
                viewModel.onSemesterStartDateSelected(selectedDateMillis)
            },
            onDismiss = { showDatePickerModal = false }
        )
    }

    if (showTotalWeeksDialog) {
        NumberPickerDialog(
            title = "选择总周数",
            range = 1..30,
            initialValue = semesterTotalWeeks,
            onDismiss = { showTotalWeeksDialog = false },
            onConfirm = { selectedWeeks ->
                viewModel.onSemesterTotalWeeksSelected(selectedWeeks)
                showTotalWeeksDialog = false
            }
        )
    }

    if (showManualWeekDialog) {
        ManualWeekPickerDialog(
            totalWeeks = semesterTotalWeeks,
            currentWeek = displayCurrentWeek,
            onDismiss = { showManualWeekDialog = false },
            onConfirm = { weekNumber ->
                viewModel.onCurrentWeekManuallySet(weekNumber)
                showManualWeekDialog = false
            }
        )
    }
}

@Composable
fun ManualWeekPickerDialog(
    totalWeeks: Int,
    currentWeek: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    val weekOptions = listOf("假期中") + (1..totalWeeks).map { "第 $it 周" }

    val initialSelectedValue = when (currentWeek) {
        null -> "假期中"
        else -> "第 $currentWeek 周"
    }

    var dialogSelectedValue by remember { mutableStateOf(initialSelectedValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动设置当前周数") },
        text = {
            NativeNumberPicker(
                values = weekOptions,
                selectedValue = dialogSelectedValue,
                onValueChange = { newValue ->
                    dialogSelectedValue = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val weekNumber = if (dialogSelectedValue == "假期中") {
                    null
                } else {
                    dialogSelectedValue.filter { it.isDigit() }.toIntOrNull()
                }
                onConfirm(weekNumber)
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialDisplayMode = DisplayMode.Picker
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            modifier = Modifier
        )
    }
}

@Composable
private fun NumberPickerDialog(
    title: String,
    range: IntRange,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dialogSelectedValue by remember { mutableStateOf(initialValue.coerceIn(range)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            NativeNumberPicker(
                values = range.toList(),
                selectedValue = initialValue.coerceIn(range),
                onValueChange = { newValue ->
                    dialogSelectedValue = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(dialogSelectedValue) }) {
                Text("确定")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}