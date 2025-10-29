package com.xingheyuzhuan.shiguangschedule.ui.settings

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xingheyuzhuan.shiguangschedule.Screen
import com.xingheyuzhuan.shiguangschedule.ui.components.BottomNavigationBar
import com.xingheyuzhuan.shiguangschedule.ui.components.DatePickerModal
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.DayOfWeek

// 常量，用于统一间距和边距
private val SETTING_PADDING = 16.dp
private val SECTION_SPACING = 16.dp
private val ITEM_SPACING = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory)
) {
    val courseTableConfig by viewModel.courseTableConfigState.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showWeekends = courseTableConfig?.showWeekends ?: false
    val semesterStartDateString = courseTableConfig?.semesterStartDate
    val semesterTotalWeeks = courseTableConfig?.semesterTotalWeeks ?: 20
    val firstDayOfWeekInt = courseTableConfig?.firstDayOfWeek ?: DayOfWeek.MONDAY.value
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
    var showFirstDayOfWeekDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("课程表设置") },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = SETTING_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING)
        ) {
            item {
                // 通用设置卡片
                GeneralSettingsSection(
                    showWeekends = showWeekends,
                    onShowWeekendsChanged = { isChecked -> viewModel.onShowWeekendsChanged(isChecked) },
                    semesterStartDate = semesterStartDate,
                    semesterTotalWeeks = semesterTotalWeeks,
                    firstDayOfWeekInt = firstDayOfWeekInt,
                    displayCurrentWeek = displayCurrentWeek,
                    onSemesterStartDateClick = { showDatePickerModal = true },
                    onSemesterTotalWeeksClick = { showTotalWeeksDialog = true },
                    onManualWeekClick = { showManualWeekDialog = true },
                    onFirstDayOfWeekClick = { showFirstDayOfWeekDialog = true },
                    onTweakScheduleClick = { navController.navigate(Screen.TweakSchedule.route) }
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp,horizontal = 16.dp),
                    thickness = 1.dp, // 设置分隔线的厚度
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            item {
                // 高级功能卡片
                AdvancedSettingsSection(navController)
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

    if (showFirstDayOfWeekDialog) {
        DayOfWeekPickerDialog(
            initialDayOfWeekInt = firstDayOfWeekInt,
            onDismiss = { showFirstDayOfWeekDialog = false },
            onConfirm = { selectedDayInt ->
                viewModel.onFirstDayOfWeekSelected(selectedDayInt)
                showFirstDayOfWeekDialog = false
            }
        )
    }
}

/**
 * 通用设置卡片
 */
@Composable
private fun GeneralSettingsSection(
    showWeekends: Boolean,
    onShowWeekendsChanged: (Boolean) -> Unit,
    semesterStartDate: LocalDate?,
    semesterTotalWeeks: Int,
    firstDayOfWeekInt: Int,
    displayCurrentWeek: Int?,
    onSemesterStartDateClick: () -> Unit,
    onSemesterTotalWeeksClick: () -> Unit,
    onManualWeekClick: () -> Unit,
    onFirstDayOfWeekClick: () -> Unit,
    onTweakScheduleClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(SETTING_PADDING),
            verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
        ) {
            Text(
                "通用设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // 显示周末设置项
            SettingItem(
                title = "是否显示周末",
                subtitle = "开启后，课程表中将显示周末的课程安排"
            ) {
                Switch(checked = showWeekends, onCheckedChange = onShowWeekendsChanged)
            }

            // 开始上课时间设置项
            SettingItem(
                title = "设置开学日期",
                subtitle = "选择学期第一周(教务系统参考的第一周)的周一",
                onClick = onSemesterStartDateClick
            ) {
                Text(
                    text = semesterStartDate?.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) ?: "未设置",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 本学期总周数设置项
            SettingItem(
                title = "本学期总周数",
                subtitle = "设置学期总共持续多少周",
                onClick = onSemesterTotalWeeksClick
            ) {
                Text(
                    text = "$semesterTotalWeeks 周",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 当前周数设置项
            SettingItem(
                title = "当前周数",
                subtitle = "手动调整当前周数",
                onClick = onManualWeekClick
            ) {
                val weekStatusText = when {
                    semesterStartDate == null -> "请设置开始日期"
                    displayCurrentWeek == null -> "假期中"
                    else -> "第 $displayCurrentWeek 周"
                }
                Text(
                    text = weekStatusText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            SettingItem(
                title = "设置每周起始日",
                subtitle = "设置一周从哪天开始计算和显示",
                onClick = onFirstDayOfWeekClick
            ) {
                val dayText = when (firstDayOfWeekInt) {
                    DayOfWeek.MONDAY.value -> "周一"
                    DayOfWeek.SUNDAY.value -> "周日"
                    else -> "周一"
                }
                Text(
                    text = dayText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 课程调动设置项
            SettingItem(
                title = "课程调动",
                subtitle = "一键将指定日期的所有课程调整到新日期",
                onClick = onTweakScheduleClick
            )
        }
    }
}

/**
 * 高级功能卡片
 */
@Composable
private fun AdvancedSettingsSection(navController: NavHostController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(SETTING_PADDING),
            verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
        ) {
            Text(
                "高级功能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            // 课表导入/导出设置项
            SettingItem(
                title = "课表导入/导出",
                subtitle = "支持多种导入导出方式",
                onClick = { navController.navigate(Screen.CourseTableConversion.route) }
            )
            // 课程提醒设置项
            SettingItem(
                title = "课程提醒设置",
                subtitle = "管理您的课程提醒通知",
                onClick = { navController.navigate(Screen.NotificationSettings.route) }
            )

            // 管理课表设置项
            SettingItem(
                title = "管理课表",
                subtitle = "管理多份课程表，可切换、删除",
                onClick = { navController.navigate(Screen.ManageCourseTables.route) }
            )

            // 自定义时间段设置项
            SettingItem(
                title = "自定义时间段",
                subtitle = "编辑您的课程时间段设置",
                onClick = { navController.navigate(Screen.TimeSlotSettings.route) }
            )

            // 更多选项设置项
            SettingItem(
                title = "更多",
                subtitle = "提供我们的更多信息",
                onClick = { navController.navigate(Screen.MoreOptions.route) },
                icon = Icons.Default.MoreHoriz
            )
        }
    }
}

/**
 * 封装单个设置项的可组合函数，提高代码复用性
 */
@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit = { Icon(icon, contentDescription = null) }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        trailingContent()
    }
}

/**
 * 手动周数选择器对话框
 */
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

/**
 * 每周起始日选择器对话框
 */
@Composable
fun DayOfWeekPickerDialog(
    initialDayOfWeekInt: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    // 选项列表，及其对应的 DayOfWeek Int 值 (1=周一, 7=周日)
    val dayOptionsMap = mapOf(
        "周一" to DayOfWeek.MONDAY.value,
        "周日" to DayOfWeek.SUNDAY.value
    )
    val dayOptions = dayOptionsMap.keys.toList()

    val initialSelectedDayText = dayOptionsMap.entries.firstOrNull { it.value == initialDayOfWeekInt }?.key
        ?: "周一" // 默认显示周一

    var dialogSelectedText by remember { mutableStateOf(initialSelectedDayText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置每周起始日") },
        text = {
            NativeNumberPicker(
                values = dayOptions,
                selectedValue = dialogSelectedText,
                onValueChange = { newValue ->
                    dialogSelectedText = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val selectedDayInt = dayOptionsMap[dialogSelectedText] ?: DayOfWeek.MONDAY.value
                onConfirm(selectedDayInt)
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


/**
 * 数字选择器对话框
 */
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