package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.StartScreen
import com.xingheyuzhuan.shiguangschedule.tool.UpdateChecker.Companion.UPDATE_CHANNELS
import com.xingheyuzhuan.shiguangschedule.tool.UpdateStatus

/**
 * 启动页面选择弹窗
 */
@Composable
fun StartScreenSelectionDialog(
    showDialog: Boolean,
    currentSelected: StartScreen,
    onDismiss: () -> Unit,
    onConfirm: (StartScreen) -> Unit
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_select_start_screen)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                StartScreen.entries.forEach { screen ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(screen) },
                        headlineContent = { Text(stringResource(screen.labelRes)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            RadioButton(selected = screen == currentSelected, onClick = null)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * 更新渠道选择弹窗
 */
@Composable
fun ChannelSelectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    currentSelectedUrl: String,
    onChannelSelected: (String) -> Unit
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_select_update_channel)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                UPDATE_CHANNELS.forEach { channel ->
                    val isSelected = channel.url == currentSelectedUrl
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChannelSelected(channel.url) },
                        headlineContent = { Text(text = channel.title) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            RadioButton(selected = isSelected, onClick = null)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(currentSelectedUrl) }) {
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

/**
 * 更新检查结果弹窗
 */
@Composable
fun UpdateResultDialog(
    showDialog: Boolean,
    updateStatus: UpdateStatus,
    onDismiss: () -> Unit,
    onDownloadClick: (String) -> Unit
) {
    if (!showDialog || updateStatus is UpdateStatus.Idle) return

    // 加载中状态
    if (updateStatus is UpdateStatus.Checking) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.dialog_checking_update)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.tip_please_wait))
                }
            },
            confirmButton = {}
        )
        return
    }

    // 结果映射
    val (title, text, confirmBtn) = when (updateStatus) {
        is UpdateStatus.Found -> Triple(
            stringResource(R.string.dialog_new_version_found, updateStatus.flavorInfo.latestVersionName),
            updateStatus.flavorInfo.changelog,
            @Composable {
                Button(onClick = { onDownloadClick(updateStatus.downloadUrl) }) {
                    Text(stringResource(R.string.btn_download_update))
                }
            }
        )
        is UpdateStatus.Latest -> Triple(
            stringResource(R.string.dialog_current_version_latest),
            stringResource(R.string.label_version_prefix, updateStatus.versionName),
            null
        )
        is UpdateStatus.Error -> Triple(
            stringResource(R.string.dialog_update_check_failed),
            stringResource(R.string.label_error_message, updateStatus.message),
            null
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { confirmBtn?.invoke() },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(if (updateStatus is UpdateStatus.Found) R.string.action_cancel else R.string.action_confirm))
            }
        }
    )
}