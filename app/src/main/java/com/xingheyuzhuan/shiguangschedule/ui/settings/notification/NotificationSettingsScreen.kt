package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import com.xingheyuzhuan.shiguangschedule.service.CourseNotificationWorker
import com.xingheyuzhuan.shiguangschedule.service.DndSchedulerWorker
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionDeniedMessage = stringResource(R.string.toast_notification_permission_denied)
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context.applicationContext, permissionDeniedMessage, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentModeText = if (!uiState.autoModeEnabled) stringResource(R.string.auto_mode_off)
    else when (uiState.autoControlMode) {
        AutoControlMode.DND -> stringResource(R.string.auto_mode_dnd)
        AutoControlMode.SILENT -> stringResource(R.string.auto_mode_silent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_course_notification_settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                GeneralSettingsCard(
                    uiState = uiState,
                    currentModeText = currentModeText,
                    onReminderToggle = { isEnabled ->
                        viewModel.onReminderEnabledChange(isEnabled, { triggerNotificationWorker(context) }, context)
                    },
                    onCompatWearableToggle = { isEnabled ->
                        viewModel.onCompatWearableSyncChange(isEnabled, { triggerNotificationWorker(context) }, context)
                    },
                    onAutoModeClick = {
                        if (uiState.reminderEnabled) viewModel.showDialog(NotificationDialogType.AutoModeSelection)
                        else Toast.makeText(context, R.string.toast_enable_reminder_first, Toast.LENGTH_SHORT).show()
                    },
                    onRemindTimeClick = { viewModel.showDialog(NotificationDialogType.EditRemindMinutes) },
                    onExactAlarmClick = { openExactAlarmSettings(context) },
                    onDndPermissionClick = { openDndSettings(context) },
                    onAppSettingsClick = { openAppSettings(context) },
                    onBatteryOptimizationClick = { openIgnoreBatteryOptimizationSettings(context) }
                )
            }
            item {
                AdvancedSettingsCard(
                    uiState = uiState,
                    onUpdateHolidays = {
                        viewModel.onUpdateHolidays(
                            onSuccess = { ctx ->
                                Toast.makeText(ctx, R.string.toast_holidays_update_success, Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { ctx, msg ->
                                Toast.makeText(ctx, ctx.getString(R.string.toast_update_failed, msg), Toast.LENGTH_LONG).show()
                            },
                            context = context
                        )
                    },
                    onClearSkippedDates = { viewModel.showDialog(NotificationDialogType.ClearConfirmation) },
                    onViewSkippedDates = { viewModel.showDialog(NotificationDialogType.ViewSkippedDates) }
                )
            }
        }
    }

    NotificationDialogDispatcher(
        uiState = uiState,
        viewModel = viewModel,
        onTriggerNotificationWorker = { triggerNotificationWorker(context) },
        onTriggerDndWorker = { triggerDndSchedulerWorker(context) }
    )
}

private fun triggerNotificationWorker(context: Context) {
    val req = OneTimeWorkRequestBuilder<CourseNotificationWorker>().build()
    WorkManager.getInstance(context).enqueueUniqueWork("CourseNotificationWorker_Settings_Update", ExistingWorkPolicy.REPLACE, req)
}

private fun triggerDndSchedulerWorker(context: Context) {
    DndSchedulerWorker.enqueueWork(context)
}