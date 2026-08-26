package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_close
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.dialog_text_clear_confirmation
import shiguangschedule.shared.generated.resources.dialog_title_clear_confirmation
import shiguangschedule.shared.generated.resources.dialog_title_set_remind_time
import shiguangschedule.shared.generated.resources.dialog_title_view_skipped_dates
import shiguangschedule.shared.generated.resources.item_course_reminder
import shiguangschedule.shared.generated.resources.item_remind_time_before
import shiguangschedule.shared.generated.resources.label_minutes_input
import shiguangschedule.shared.generated.resources.remind_time_minutes_format
import shiguangschedule.shared.generated.resources.section_title_general
import shiguangschedule.shared.generated.resources.skipped_dates_none
import shiguangschedule.shared.generated.resources.toast_clear_failed
import shiguangschedule.shared.generated.resources.toast_clear_success

@Composable
fun IosGeneralSettingsCard(
    uiState: NotificationSettingsUiState,
    onReminderToggle: (Boolean) -> Unit,
    onRemindTimeClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.section_title_general), style = MaterialTheme.typography.titleLarge)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingSwitch(stringResource(Res.string.item_course_reminder), uiState.reminderEnabled, onReminderToggle)
                SettingItemRow(title = stringResource(Res.string.item_remind_time_before), currentValue = stringResource(Res.string.remind_time_minutes_format, uiState.remindBeforeMinutes), onClick = onRemindTimeClick)
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun IosNotificationDialogs(uiState: NotificationSettingsUiState, viewModel: NotificationSettingsViewModel) {
    val coroutineScope = rememberCoroutineScope()

    when (uiState.activeDialog) {
        NotificationDialogType.EditRemindMinutes -> {
            var tempInput by remember(uiState.remindBeforeMinutes) {
                mutableStateOf(uiState.remindBeforeMinutes.toString())
            }
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(Res.string.dialog_title_set_remind_time)) },
                text = {
                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { tempInput = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(Res.string.label_minutes_input)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button({
                        viewModel.updateRemindBeforeMinutes(tempInput.toIntOrNull() ?: 15)
                    }) { Text(stringResource(Res.string.action_confirm)) }
                },
                dismissButton = { TextButton(viewModel::dismissDialog) { Text(stringResource(Res.string.action_cancel)) } }
            )
        }
        NotificationDialogType.ClearConfirmation -> {
            val successMsg = stringResource(Res.string.toast_clear_success)

            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(Res.string.dialog_title_clear_confirmation)) },
                text = { Text(stringResource(Res.string.dialog_text_clear_confirmation)) },
                confirmButton = {
                    Button({
                        viewModel.clearSkippedDates { result ->
                            result.fold(
                                onSuccess = { ToastManager.show(successMsg) },
                                onFailure = { e ->
                                    coroutineScope.launch {
                                        val errorMsg = getString(Res.string.toast_clear_failed, e.message ?: "")
                                        ToastManager.show(errorMsg)
                                    }
                                }
                            )
                        }
                        viewModel.dismissDialog()
                    }) { Text(stringResource(Res.string.action_confirm)) }
                },
                dismissButton = { TextButton(viewModel::dismissDialog) { Text(stringResource(Res.string.action_cancel)) } }
            )
        }
        NotificationDialogType.ViewSkippedDates -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(Res.string.dialog_title_view_skipped_dates)) },
                text = {
                    if (uiState.skippedDates.isEmpty()) {
                        Text(stringResource(Res.string.skipped_dates_none))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(100.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(uiState.skippedDates.toList().sorted()) { date ->
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
                    Button(viewModel::dismissDialog) { Text(stringResource(Res.string.action_close)) }
                }
            )
        }
        else -> Unit
    }
}
