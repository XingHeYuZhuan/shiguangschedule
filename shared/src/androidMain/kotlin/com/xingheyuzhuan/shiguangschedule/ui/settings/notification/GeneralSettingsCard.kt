package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.desc_compat_wearable_sync
import shiguangschedule.shared.generated.resources.item_auto_mode
import shiguangschedule.shared.generated.resources.item_background_and_autostart
import shiguangschedule.shared.generated.resources.item_compat_wearable_sync
import shiguangschedule.shared.generated.resources.item_course_reminder
import shiguangschedule.shared.generated.resources.item_dnd_permission
import shiguangschedule.shared.generated.resources.item_exact_alarm_permission
import shiguangschedule.shared.generated.resources.item_ignore_battery_optimization
import shiguangschedule.shared.generated.resources.item_remind_time_before
import shiguangschedule.shared.generated.resources.remind_time_minutes_format
import shiguangschedule.shared.generated.resources.section_title_general
import shiguangschedule.shared.generated.resources.status_authorized
import shiguangschedule.shared.generated.resources.status_disabled
import shiguangschedule.shared.generated.resources.status_enabled
import shiguangschedule.shared.generated.resources.status_unauthorized
import shiguangschedule.shared.generated.resources.text_auto_mode_dependency
import shiguangschedule.shared.generated.resources.text_permission_importance_detail
import shiguangschedule.shared.generated.resources.text_permission_importance_title

/**
 * 常规设置卡片 UI 组件 (Android 专属)
 */
@Composable
fun GeneralSettingsCard(
    uiState: NotificationSettingsUiState,
    currentModeText: String?,
    onReminderToggle: (Boolean) -> Unit,
    onCompatWearableToggle: (Boolean) -> Unit,
    onAutoModeClick: () -> Unit,
    onRemindTimeClick: () -> Unit,
    onAppSettingsClick: () -> Unit,
    onBatteryOptimizationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 本地维护 Android 专属权限弹窗的显隐状态，不污染全平台 ViewModel
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var showDndDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.section_title_general),
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
                    text = stringResource(Res.string.text_permission_importance_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.text_permission_importance_detail),
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
                        text = stringResource(Res.string.item_course_reminder),
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
                            text = stringResource(Res.string.item_compat_wearable_sync),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = uiState.compatWearableSync,
                            onCheckedChange = onCompatWearableToggle
                        )
                    }
                    Text(
                        text = stringResource(Res.string.desc_compat_wearable_sync),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }

                HorizontalDivider()

                // 3. 上课自动模式
                SettingItemRow(
                    title = stringResource(Res.string.item_auto_mode),
                    currentValue = currentModeText,
                    onClick = onAutoModeClick
                )
                if (!uiState.reminderEnabled) {
                    Text(
                        text = stringResource(Res.string.text_auto_mode_dependency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
                    )
                }

                HorizontalDivider()

                // 4. 提前提醒时间
                SettingItemRow(
                    title = stringResource(Res.string.item_remind_time_before),
                    currentValue = stringResource(Res.string.remind_time_minutes_format, uiState.remindBeforeMinutes),
                    onClick = onRemindTimeClick
                )

                // 5. 精确闹钟权限 (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider()
                    val statusText = if (uiState.exactAlarmStatus)
                        stringResource(Res.string.status_enabled)
                    else
                        stringResource(Res.string.status_disabled)

                    SettingItemRow(
                        title = stringResource(Res.string.item_exact_alarm_permission),
                        currentValue = statusText,
                        onClick = { showExactAlarmDialog = true }
                    )
                }

                HorizontalDivider()

                // 6. 勿扰模式权限
                val dndStatusText = if (uiState.dndPermissionStatus)
                    stringResource(Res.string.status_authorized)
                else
                    stringResource(Res.string.status_unauthorized)

                SettingItemRow(
                    title = stringResource(Res.string.item_dnd_permission),
                    currentValue = dndStatusText,
                    onClick = { showDndDialog = true }
                )

                HorizontalDivider()

                // 7. 后台与自启动
                SettingItemRow(
                    title = stringResource(Res.string.item_background_and_autostart),
                    onClick = onAppSettingsClick
                )

                HorizontalDivider()

                // 8. 忽略电池优化
                SettingItemRow(
                    title = stringResource(Res.string.item_ignore_battery_optimization),
                    onClick = onBatteryOptimizationClick
                )
            }
        }
    }

    // 渲染 Android 专属权限弹窗组件
    if (showExactAlarmDialog) {
        ExactAlarmPermissionGuideDialog(
            onDismiss = { showExactAlarmDialog = false }
        )
    }

    if (showDndDialog) {
        DndPermissionGuideDialog(
            onDismiss = { showDndDialog = false }
        )
    }
}