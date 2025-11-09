package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.service.CourseAlarmReceiver
import com.xingheyuzhuan.shiguangschedule.service.CourseNotificationWorker
import com.xingheyuzhuan.shiguangschedule.service.DndSchedulerWorker

// 这是一个用于跳转到精确闹钟设置的通用函数
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        context.startActivity(intent)
    } else {
        // 对于旧版本设备，跳转到应用详情页
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri()
        )
        context.startActivity(intent)
    }
}

// 检查是否拥有精确闹钟权限的函数
fun hasExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService<AlarmManager>()
        alarmManager?.canScheduleExactAlarms() ?: false
    } else {
        // API < 31 的设备不需要此权限
        true
    }
}

// 检查是否拥有通知权限的函数
fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

// 检查是否拥有勿扰模式权限的函数
fun hasDndPermission(context: Context): Boolean {
    val notificationManager = context.getSystemService<NotificationManager>()
    return notificationManager?.isNotificationPolicyAccessGranted ?: false
}

// 跳转到勿扰模式设置的函数
fun openDndSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    context.startActivity(intent)
}


// 触发 Notification Worker 的辅助函数
private fun triggerNotificationWorker(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<CourseNotificationWorker>().build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "CourseNotificationWorker_Settings_Update", // 确保唯一名称
        ExistingWorkPolicy.REPLACE,
        workRequest
    )
}

// 触发 DND Worker 的辅助函数
private fun triggerDndSchedulerWorker(context: Context) {
    DndSchedulerWorker.enqueueWork(context)
}


// 跳转到应用信息页面的辅助函数
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent)
}

// 跳转到忽略电池优化设置的辅助函数
fun openIgnoreBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 使用 ViewModel 的静态工厂方法实例化 ViewModel
    val viewModel: NotificationSettingsViewModel = viewModel(
        factory = NotificationSettingsViewModel.provideFactory(context.applicationContext as Application)
    )

    val uiState by viewModel.uiState.collectAsState()

    // --- 状态定义 ---
    var showEditRemindMinutesDialog by remember { mutableStateOf(false) }
    var showViewSkippedDatesDialog by remember { mutableStateOf(false) }
    var tempRemindMinutesInput by remember { mutableStateOf(uiState.remindBeforeMinutes.toString()) }
    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var showDndPermissionDialog by remember { mutableStateOf(false) }
    var showAutoModeSelectionDialog by remember { mutableStateOf(false) }

    // 模式选项
    val modeOptions = mapOf(
        "OFF" to stringResource(R.string.auto_mode_off),
        CourseAlarmReceiver.MODE_DND to stringResource(R.string.auto_mode_dnd),
        CourseAlarmReceiver.MODE_SILENT to stringResource(R.string.auto_mode_silent)
    )

    // 根据当前状态获取显示文本
    val currentModeText = remember(uiState.autoModeEnabled, uiState.autoControlMode) {
        if (!uiState.autoModeEnabled) {
            modeOptions["OFF"]
        } else {
            modeOptions[uiState.autoControlMode]
        }
    }

    // --- 权限请求器 ---
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.toast_notification_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    // --- 数据加载和权限检查 ---
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateExactAlarmStatus(hasExactAlarmPermission(context))
                viewModel.updateDndPermissionStatus(hasDndPermission(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_course_notification_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                // 第一个卡片组：提醒功能、权限和后台设置
                Text(stringResource(R.string.section_title_general), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.text_permission_importance_title), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.text_permission_importance_detail), style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        // 卡片 1: 课程提醒开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.item_course_reminder), style = MaterialTheme.typography.titleMedium)
                            Switch(
                                checked = uiState.reminderEnabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.onReminderEnabledChange(
                                        isEnabled,
                                        uiState.exactAlarmStatus,
                                        ::triggerNotificationWorker,
                                        { showExactAlarmPermissionDialog = true },
                                        context
                                    )
                                }
                            )
                        }
                        HorizontalDivider()

                        SettingItemRow(
                            title = stringResource(R.string.item_auto_mode),
                            currentValue = currentModeText,
                            onClick = {
                                if (!uiState.reminderEnabled) {
                                    Toast.makeText(context, context.getString(R.string.toast_enable_reminder_first), Toast.LENGTH_SHORT).show()
                                } else {
                                    showAutoModeSelectionDialog = true
                                }
                            }
                        )
                        if (!uiState.reminderEnabled) {
                            Text(
                                text = stringResource(R.string.text_auto_mode_dependency),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
                            )
                        }
                        HorizontalDivider()


                        SettingItemRow(
                            title = stringResource(R.string.item_remind_time_before),
                            currentValue = stringResource(R.string.remind_time_minutes_format, uiState.remindBeforeMinutes),
                            onClick = {
                                tempRemindMinutesInput = uiState.remindBeforeMinutes.toString()
                                showEditRemindMinutesDialog = true
                            }
                        )

                        // 精确闹钟权限 (Android 12+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val statusText = if (uiState.exactAlarmStatus) stringResource(R.string.status_enabled) else stringResource(R.string.status_disabled)
                            SettingItemRow(
                                title = stringResource(R.string.item_exact_alarm_permission),
                                currentValue = statusText,
                                onClick = { openExactAlarmSettings(context) }
                            )
                        }

                        // 上课勿扰模式权限设置项
                        HorizontalDivider()
                        val dndStatusText = if (uiState.dndPermissionStatus) stringResource(R.string.status_authorized) else stringResource(R.string.status_unauthorized)
                        SettingItemRow(
                            title = stringResource(R.string.item_dnd_permission),
                            currentValue = dndStatusText,
                            onClick = { openDndSettings(context) }
                        )

                        HorizontalDivider()
                        SettingItemRow(
                            title = stringResource(R.string.item_background_and_autostart),
                            onClick = { openAppSettings(context) }
                        )
                        HorizontalDivider()
                        SettingItemRow(
                            title = stringResource(R.string.item_ignore_battery_optimization),
                            onClick = { openIgnoreBatteryOptimizationSettings(context) }
                        )
                    }
                }
            }

            item {
                Text(stringResource(R.string.section_title_advanced), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.section_title_skip_dates), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.text_skip_dates_experimental), style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        SettingItemRow(
                            title = stringResource(R.string.item_update_holiday_info),
                            currentValue = null,
                            onClick = {
                                viewModel.onUpdateHolidays(
                                    onSuccess = { ctx -> Toast.makeText(ctx, ctx.getString(R.string.toast_holidays_update_success), Toast.LENGTH_SHORT).show() },
                                    onFailure = { ctx, msg -> Toast.makeText(ctx, ctx.getString(R.string.toast_update_failed, msg), Toast.LENGTH_LONG).show() },
                                    context
                                )
                            }
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 5.dp
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.update_holiday_info_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        HorizontalDivider()

                        SettingItemRow(
                            title = stringResource(R.string.item_clear_skipped_dates),
                            currentValue = null,
                            onClick = { showClearConfirmationDialog = true }
                        )
                        HorizontalDivider()

                        SettingItemRow(
                            title = stringResource(R.string.item_view_skipped_dates),
                            currentValue = if (uiState.skippedDates.isNotEmpty()) {
                                stringResource(R.string.skipped_dates_count_format, uiState.skippedDates.size)
                            } else {
                                stringResource(R.string.skipped_dates_none)
                            },
                            onClick = { showViewSkippedDatesDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showAutoModeSelectionDialog) {
        AutoModeSelectionDialog(
            modeOptions = modeOptions,
            currentAutoModeEnabled = uiState.autoModeEnabled,
            currentAutoControlMode = uiState.autoControlMode,
            hasDndPermission = uiState.dndPermissionStatus,
            onModeSelected = { selectedKey ->
                val isEnabled = selectedKey != "OFF"
                val controlMode = if (isEnabled) selectedKey else uiState.autoControlMode

                if (isEnabled && !uiState.dndPermissionStatus) {
                    showDndPermissionDialog = true
                    return@AutoModeSelectionDialog
                }

                viewModel.onAutoModeStateChange(
                    isEnabled = isEnabled,
                    newControlMode = controlMode,
                    triggerDndWorker = ::triggerDndSchedulerWorker,
                    context = context
                )

                showAutoModeSelectionDialog = false
            },
            onDismiss = { showAutoModeSelectionDialog = false }
        )
    }


    if (showEditRemindMinutesDialog) {
        EditRemindMinutesDialog(
            currentMinutes = tempRemindMinutesInput,
            onMinutesChange = { tempRemindMinutesInput = it.filter { char -> char.isDigit() } },
            onConfirm = {
                val newMinutes = tempRemindMinutesInput.toIntOrNull() ?: 0
                viewModel.onSaveRemindBeforeMinutes(newMinutes, ::triggerNotificationWorker, context)
                showEditRemindMinutesDialog = false
            },
            onDismiss = { showEditRemindMinutesDialog = false }
        )
    }

    if (showViewSkippedDatesDialog) {
        ViewSkippedDatesDialog(
            dates = uiState.skippedDates,
            onDismiss = { showViewSkippedDatesDialog = false }
        )
    }

    if (showExactAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmPermissionDialog = false },
            title = { Text(stringResource(R.string.dialog_title_exact_alarm_permission)) },
            text = { Text(stringResource(R.string.dialog_text_exact_alarm_permission)) },
            confirmButton = {
                Button(onClick = {
                    showExactAlarmPermissionDialog = false
                    openExactAlarmSettings(context)
                }) {
                    Text(stringResource(R.string.action_go_to_settings))
                }
            },
            dismissButton = {
                Button(onClick = { showExactAlarmPermissionDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = { Text(stringResource(R.string.dialog_title_clear_confirmation)) },
            text = { Text(stringResource(R.string.dialog_text_clear_confirmation)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.onClearSkippedDates(
                        onSuccess = { ctx -> Toast.makeText(ctx, ctx.getString(R.string.toast_clear_success), Toast.LENGTH_SHORT).show() },
                        onFailure = { ctx, msg -> Toast.makeText(ctx, ctx.getString(R.string.toast_clear_failed, msg), Toast.LENGTH_LONG).show() },
                        context
                    )
                    showClearConfirmationDialog = false
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                Button(onClick = { showClearConfirmationDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDndPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showDndPermissionDialog = false },
            title = { Text(stringResource(R.string.dialog_title_dnd_permission)) },
            text = { Text(stringResource(R.string.dialog_text_dnd_permission)) },
            confirmButton = {
                Button(onClick = {
                    showDndPermissionDialog = false
                    openDndSettings(context)
                }) {
                    Text(stringResource(R.string.action_go_to_settings))
                }
            },
            dismissButton = {
                Button(onClick = { showDndPermissionDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
@Composable
fun AutoModeSelectionDialog(
    modeOptions: Map<String, String>,
    currentAutoModeEnabled: Boolean,
    currentAutoControlMode: String,
    hasDndPermission: Boolean,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedKey by remember {
        mutableStateOf(if (currentAutoModeEnabled) currentAutoControlMode else "OFF")
    }

    // 权限提示文本
    val permissionText = if (!hasDndPermission) stringResource(R.string.auto_mode_dnd_permission_warning) else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_auto_mode_selection)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_title_auto_mode_selection) + permissionText, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                modeOptions.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedKey = key }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedKey == key,
                            onClick = { selectedKey = key }
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onModeSelected(selectedKey) }
            ) {
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

@Composable
fun SettingItemRow(
    title: String,
    currentValue: String? = null,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailing()
            currentValue?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.a11y_details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EditRemindMinutesDialog(
    currentMinutes: String,
    onMinutesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val errorText = if (currentMinutes.isEmpty()) {
        stringResource(R.string.error_input_empty)
    } else if (currentMinutes.toIntOrNull() !in 0..60) {
        stringResource(R.string.error_input_range)
    } else {
        null
    }
    val isInputValid = errorText == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_set_remind_time)) },
        text = {
            Column {
                OutlinedTextField(
                    value = currentMinutes,
                    onValueChange = {
                        onMinutesChange(it)
                    },
                    label = { Text(stringResource(R.string.label_minutes_input)) },
                    singleLine = true,
                    isError = errorText != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isInputValid
            ) {
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

@Composable
fun ViewSkippedDatesDialog(
    dates: Set<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_view_skipped_dates)) },
        text = {
            if (dates.isEmpty()) {
                Text(stringResource(R.string.text_no_skipped_dates))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dates.toList().sorted()) { dateString ->
                        Text(
                            text = dateString,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}