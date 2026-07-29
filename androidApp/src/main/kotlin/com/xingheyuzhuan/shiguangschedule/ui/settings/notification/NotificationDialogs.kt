package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode

@Composable
fun NotificationDialogDispatcher(
    uiState: NotificationSettingsUiState,
    viewModel: NotificationSettingsViewModel,
    onTriggerNotificationWorker: () -> Unit,
    onTriggerDndWorker: () -> Unit
) {
    val context = LocalContext.current

    when (uiState.activeDialog) {
        is NotificationDialogType.EditRemindMinutes -> {
            var tempInput by remember(uiState.remindBeforeMinutes) {
                mutableStateOf(uiState.remindBeforeMinutes.toString())
            }
            EditRemindMinutesDialog(
                currentMinutes = tempInput,
                onMinutesChange = { tempInput = it.filter { c -> c.isDigit() } },
                onConfirm = {
                    val mins = tempInput.toIntOrNull() ?: 15
                    viewModel.onSaveRemindBeforeMinutes(mins, { onTriggerNotificationWorker() }, context)
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is NotificationDialogType.AutoModeSelection -> {
            AutoModeSelectionDialog(
                currentAutoModeEnabled = uiState.autoModeEnabled,
                currentAutoControlMode = uiState.autoControlMode,
                hasDndPermission = uiState.dndPermissionStatus,
                onModeSelected = { selectedKey ->
                    if (selectedKey == "OFF") {
                        viewModel.onAutoModeStateChange(false, uiState.autoControlMode, { onTriggerDndWorker() }, context)
                    } else if (selectedKey is AutoControlMode) {
                        viewModel.onAutoModeStateChange(true, selectedKey, { onTriggerDndWorker() }, context)
                    }
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is NotificationDialogType.ClearConfirmation -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text(stringResource(R.string.dialog_title_clear_confirmation)) },
                text = { Text(stringResource(R.string.dialog_text_clear_confirmation)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.onClearSkippedDates(
                            onSuccess = {
                                Toast.makeText(it, it.getString(R.string.toast_clear_success), Toast.LENGTH_SHORT).show()
                                viewModel.dismissDialog()
                            },
                            onFailure = { it, msg ->
                                Toast.makeText(it, it.getString(R.string.toast_clear_failed, msg), Toast.LENGTH_LONG).show()
                            },
                            context = context
                        )
                    }) { Text(stringResource(R.string.action_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }

        is NotificationDialogType.ViewSkippedDates -> {
            ViewSkippedDatesDialog(
                dates = uiState.skippedDates,
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is NotificationDialogType.ExactAlarmPermission -> {
            PermissionGuideDialog(
                title = stringResource(R.string.dialog_title_exact_alarm_permission),
                text = stringResource(R.string.dialog_text_exact_alarm_permission),
                onConfirm = { openExactAlarmSettings(context) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is NotificationDialogType.DndPermission -> {
            PermissionGuideDialog(
                title = stringResource(R.string.dialog_title_dnd_permission),
                text = stringResource(R.string.dialog_text_dnd_permission),
                onConfirm = { openDndSettings(context) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        else -> {}
    }
}

@Composable
fun AutoModeSelectionDialog(
    currentAutoModeEnabled: Boolean,
    currentAutoControlMode: AutoControlMode,
    hasDndPermission: Boolean,
    onModeSelected: (Any) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedKey by remember { mutableStateOf<Any>(if (currentAutoModeEnabled) currentAutoControlMode else "OFF") }

    val modeOptions = listOf(
        "OFF" to stringResource(R.string.auto_mode_off),
        AutoControlMode.DND to stringResource(R.string.auto_mode_dnd),
        AutoControlMode.SILENT to stringResource(R.string.auto_mode_silent)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_auto_mode_selection)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!hasDndPermission) {
                    Text(
                        text = stringResource(R.string.auto_mode_dnd_permission_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                modeOptions.forEach { (optionKey, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedKey = optionKey }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = (selectedKey == optionKey), onClick = { selectedKey = optionKey })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onModeSelected(selectedKey) }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun EditRemindMinutesDialog(
    currentMinutes: String,
    onMinutesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_set_remind_time)) },
        text = {
            OutlinedTextField(
                value = currentMinutes,
                onValueChange = onMinutesChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.label_minutes_input)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun ViewSkippedDatesDialog(dates: Set<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_view_skipped_dates)) },
        text = {
            if (dates.isEmpty()) {
                Text(stringResource(R.string.skipped_dates_none))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(dates.toList().sorted()) { date ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = date,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

/**
 * 权限引导弹窗
 */
@Composable
fun PermissionGuideDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(stringResource(R.string.action_go_to_settings)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}