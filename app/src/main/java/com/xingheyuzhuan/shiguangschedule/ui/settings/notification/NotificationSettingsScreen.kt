package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.Manifest
import android.app.AlarmManager
import android.app.Application
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
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
import com.xingheyuzhuan.shiguangschedule.service.CourseNotificationWorker

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

// 触发 Worker 的辅助函数
private fun triggerNotificationWorker(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<CourseNotificationWorker>().build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "CourseNotificationWorker_Settings_Update", // 确保唯一名称
        ExistingWorkPolicy.REPLACE,
        workRequest
    )
}

// 新增：跳转到应用信息页面的辅助函数
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent)
}

// 新增：跳转到忽略电池优化设置的辅助函数
fun openIgnoreBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
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

    // --- 权限请求器 ---
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, "通知权限被拒绝，无法接收课程提醒。", Toast.LENGTH_LONG).show()
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
                title = { Text("课程提醒设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 第一个卡片组：提醒功能、权限和后台设置
                Text("常规", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("权限设置对于 Android 提醒非常重要", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("为了确保提醒按时送达，你需要：\n1. 授予精确闹钟权限 (Android 12及以上)\n2. 开启应用的自启动权限\n3. 关闭应用的电池优化功能\n\n请在下方的设置项中手动开启这些权限。", style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        // 卡片 1: 提醒设置
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("课程提醒", style = MaterialTheme.typography.titleMedium)
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
                            title = "提前提醒时间",
                            currentValue = "${uiState.remindBeforeMinutes} 分钟",
                            onClick = {
                                tempRemindMinutesInput = uiState.remindBeforeMinutes.toString()
                                showEditRemindMinutesDialog = true
                            }
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val statusText = if (uiState.exactAlarmStatus) "已开启" else "未开启"
                            SettingItemRow(
                                title = "精确闹钟权限",
                                currentValue = statusText,
                                onClick = { openExactAlarmSettings(context) }
                            )
                        }
                        HorizontalDivider()
                        SettingItemRow(
                            title = "后台运行和自启",
                            onClick = { openAppSettings(context) }
                        )
                        HorizontalDivider()
                        SettingItemRow(
                            title = "忽略电池优化",
                            onClick = { openIgnoreBatteryOptimizationSettings(context) }
                        )
                    }
                }
            }

            item {
                Text("高级", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("跳过日期功能", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("实验性的功能,不保证一定有用", style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        SettingItemRow(
                            title = "更新节假日信息",
                            currentValue = null,
                            onClick = {
                                viewModel.onUpdateHolidays(
                                    onSuccess = { ctx -> Toast.makeText(ctx, "节假日信息更新成功！", Toast.LENGTH_SHORT).show() },
                                    onFailure = { ctx, msg -> Toast.makeText(ctx, "更新失败：$msg", Toast.LENGTH_LONG).show() },
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
                            text = "可能不稳定(一般情况下只有年末才公布下一年的节假日信息)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        HorizontalDivider()

                        SettingItemRow(
                            title = "清空跳过日期",
                            currentValue = null,
                            onClick = { showClearConfirmationDialog = true }
                        )
                        HorizontalDivider()

                        SettingItemRow(
                            title = "查看跳过的日期",
                            currentValue = if (uiState.skippedDates.isNotEmpty()) "${uiState.skippedDates.size} 个日期" else "无",
                            onClick = { showViewSkippedDatesDialog = true }
                        )
                    }
                }
            }
        }
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
            title = { Text("需要精确闹钟权限") },
            text = { Text("为了确保提醒能在准确时间送达，请授予精确闹钟权限。") },
            confirmButton = {
                Button(onClick = {
                    showExactAlarmPermissionDialog = false
                    openExactAlarmSettings(context)
                }) {
                    Text("去设置")
                }
            },
            dismissButton = {
                Button(onClick = { showExactAlarmPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = { Text("确认清空") },
            text = { Text("这会永久清除所有跳过的日期，无法恢复。确定要继续吗？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.onClearSkippedDates(
                        onSuccess = { ctx -> Toast.makeText(ctx, "已清空所有跳过日期！", Toast.LENGTH_SHORT).show() },
                        onFailure = { ctx, msg -> Toast.makeText(ctx, "清空失败：$msg", Toast.LENGTH_LONG).show() },
                        context
                    )
                    showClearConfirmationDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(onClick = { showClearConfirmationDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
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
                contentDescription = "详情",
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
    val isInputValid = currentMinutes.toIntOrNull() in 0..60
    val errorText = if (currentMinutes.isEmpty()) "请输入分钟数" else if (!isInputValid) "请输入0到60之间的数字" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置提前提醒时间") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentMinutes,
                    onValueChange = {
                        onMinutesChange(it)
                    },
                    label = { Text("分钟数") },
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

@Composable
fun ViewSkippedDatesDialog(
    dates: Set<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳过的提醒日期") },
        text = {
            if (dates.isEmpty()) {
                Text("没有设置跳过的日期。")
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
                Text("关闭")
            }
        }
    )
}

@Preview(showBackground = true, name = "Notification Settings Screen Preview")
@Composable
fun NotificationSettingsScreenPreview() {
    NotificationSettingsScreen(
        onNavigateBack = {}
    )
}