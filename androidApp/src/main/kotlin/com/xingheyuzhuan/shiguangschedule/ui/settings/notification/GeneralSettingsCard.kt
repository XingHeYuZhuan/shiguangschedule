package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.R

@Composable
fun GeneralSettingsCard(
    uiState: NotificationSettingsUiState,
    currentModeText: String?,
    onReminderToggle: (Boolean) -> Unit,
    onCompatWearableToggle: (Boolean) -> Unit,
    onAutoModeClick: () -> Unit,
    onRemindTimeClick: () -> Unit,
    onExactAlarmClick: () -> Unit,
    onDndPermissionClick: () -> Unit,
    onAppSettingsClick: () -> Unit,
    onBatteryOptimizationClick: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.section_title_general),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 权限说明头部
                Text(
                    text = stringResource(R.string.text_permission_importance_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.text_permission_importance_detail),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 1. 上课提醒主开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.item_course_reminder),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = uiState.reminderEnabled,
                        onCheckedChange = onReminderToggle
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 2. 兼容穿戴设备同步通知开关
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.item_compat_wearable_sync),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = uiState.compatWearableSync,
                            onCheckedChange = onCompatWearableToggle
                        )
                    }
                    Text(
                        text = stringResource(R.string.desc_compat_wearable_sync),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }

                HorizontalDivider()

                // 3. 上课自动模式
                SettingItemRow(
                    title = stringResource(R.string.item_auto_mode),
                    currentValue = currentModeText,
                    onClick = onAutoModeClick
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

                // 4. 提前提醒时间
                SettingItemRow(
                    title = stringResource(R.string.item_remind_time_before),
                    currentValue = stringResource(R.string.remind_time_minutes_format, uiState.remindBeforeMinutes),
                    onClick = onRemindTimeClick
                )

                // 5. 精确闹钟权限 (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider()
                    val statusText = if (uiState.exactAlarmStatus)
                        stringResource(R.string.status_enabled)
                    else
                        stringResource(R.string.status_disabled)

                    SettingItemRow(
                        title = stringResource(R.string.item_exact_alarm_permission),
                        currentValue = statusText,
                        onClick = onExactAlarmClick
                    )
                }

                HorizontalDivider()

                // 6. 勿扰模式权限
                val dndStatusText = if (uiState.dndPermissionStatus)
                    stringResource(R.string.status_authorized)
                else
                    stringResource(R.string.status_unauthorized)

                SettingItemRow(
                    title = stringResource(R.string.item_dnd_permission),
                    currentValue = dndStatusText,
                    onClick = onDndPermissionClick
                )

                HorizontalDivider()

                // 7. 后台与自启动
                SettingItemRow(
                    title = stringResource(R.string.item_background_and_autostart),
                    onClick = onAppSettingsClick
                )

                HorizontalDivider()

                // 8. 忽略电池优化
                SettingItemRow(
                    title = stringResource(R.string.item_ignore_battery_optimization),
                    onClick = onBatteryOptimizationClick
                )
            }
        }
    }
}