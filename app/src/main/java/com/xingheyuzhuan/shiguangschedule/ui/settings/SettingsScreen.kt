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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.Screen
import com.xingheyuzhuan.shiguangschedule.ui.components.BottomNavigationBar
import com.xingheyuzhuan.shiguangschedule.ui.components.DatePickerModal
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
                title = { Text(stringResource(R.string.title_schedule_settings)) },
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
            title = stringResource(R.string.dialog_title_select_total_weeks),
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
                stringResource(R.string.section_title_general_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // 显示周末设置项
            SettingItem(
                title = stringResource(R.string.item_show_weekends),
                subtitle = stringResource(R.string.desc_show_weekends)
            ) {
                Switch(checked = showWeekends, onCheckedChange = onShowWeekendsChanged)
            }

            // 开始上课时间设置项
            SettingItem(
                title = stringResource(R.string.item_set_start_date),
                subtitle = stringResource(R.string.desc_set_start_date),
                onClick = onSemesterStartDateClick
            ) {
                Text(
                    text = semesterStartDate?.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_format_year_month_day)))
                        ?: stringResource(R.string.status_not_set),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 本学期总周数设置项
            SettingItem(
                title = stringResource(R.string.item_total_weeks),
                subtitle = stringResource(R.string.desc_total_weeks),
                onClick = onSemesterTotalWeeksClick
            ) {
                Text(
                    text = stringResource(R.string.status_total_weeks_format, semesterTotalWeeks),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 当前周数设置项
            SettingItem(
                title = stringResource(R.string.item_current_week),
                subtitle = stringResource(R.string.desc_current_week_manual),
                onClick = onManualWeekClick
            ) {
                val weekStatusText = when {
                    semesterStartDate == null -> stringResource(R.string.status_set_start_date_first)
                    displayCurrentWeek == null -> stringResource(R.string.status_on_vacation)
                    else -> stringResource(R.string.status_current_week_format, displayCurrentWeek)
                }
                Text(
                    text = weekStatusText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            SettingItem(
                title = stringResource(R.string.item_first_day_of_week),
                subtitle = stringResource(R.string.desc_first_day_of_week),
                onClick = onFirstDayOfWeekClick
            ) {
                val dayText = when (firstDayOfWeekInt) {
                    DayOfWeek.MONDAY.value -> stringResource(R.string.day_of_week_monday)
                    DayOfWeek.SUNDAY.value -> stringResource(R.string.day_of_week_sunday)
                    else -> stringResource(R.string.day_of_week_monday)
                }
                Text(
                    text = dayText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 课程调动设置项
            SettingItem(
                title = stringResource(R.string.item_schedule_tweak),
                subtitle = stringResource(R.string.desc_schedule_tweak),
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
                stringResource(R.string.section_title_advanced_features),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            // 课表导入/导出设置项
            SettingItem(
                title = stringResource(R.string.item_course_conversion),
                subtitle = stringResource(R.string.desc_course_conversion),
                onClick = { navController.navigate(Screen.CourseTableConversion.route) }
            )
            // 课程提醒设置项
            SettingItem(
                title = stringResource(R.string.title_course_notification_settings),
                subtitle = stringResource(R.string.desc_notification_settings),
                onClick = { navController.navigate(Screen.NotificationSettings.route) }
            )

            // 管理课表设置项
            SettingItem(
                title = stringResource(R.string.title_manage_course_tables),
                subtitle = stringResource(R.string.desc_manage_course_tables),
                onClick = { navController.navigate(Screen.ManageCourseTables.route) }
            )

            // 自定义时间段设置项
            SettingItem(
                title = stringResource(R.string.item_time_slot_customization),
                subtitle = stringResource(R.string.desc_time_slot_customization),
                onClick = { navController.navigate(Screen.TimeSlotSettings.route) }
            )

            // 更多选项设置项
            SettingItem(
                title = stringResource(R.string.item_more_options),
                subtitle = stringResource(R.string.desc_more_options),
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
    val optionOnVacationText = stringResource(R.string.dialog_option_on_vacation)

    // 构建选项列表
    val weekOptions = listOf(optionOnVacationText) + (1..totalWeeks).map { stringResource(R.string.status_current_week_format, it) }

    val initialSelectedValue = when (currentWeek) {
        null -> optionOnVacationText
        else -> stringResource(R.string.status_current_week_format, currentWeek)
    }

    var dialogSelectedValue by remember { mutableStateOf(initialSelectedValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_manual_set_week)) },
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
                val weekNumber = if (dialogSelectedValue == optionOnVacationText) {
                    null
                } else {
                    dialogSelectedValue.filter { it.isDigit() }.toIntOrNull()
                }
                onConfirm(weekNumber)
            }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
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
    val dayOfWeekMondayText = stringResource(R.string.day_of_week_monday)
    val dayOfWeekSundayText = stringResource(R.string.day_of_week_sunday)

    // 选项列表，及其对应的 DayOfWeek Int 值 (1=周一, 7=周日)
    val dayOptionsMap = mapOf(
        dayOfWeekMondayText to DayOfWeek.MONDAY.value,
        dayOfWeekSundayText to DayOfWeek.SUNDAY.value
    )
    val dayOptions = dayOptionsMap.keys.toList()

    val initialSelectedDayText = dayOptionsMap.entries.firstOrNull { it.value == initialDayOfWeekInt }?.key
        ?: dayOfWeekMondayText // 默认显示周一

    var dialogSelectedText by remember { mutableStateOf(initialSelectedDayText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_set_first_day_of_week)) },
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
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
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
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}