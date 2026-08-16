package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_next_step
import shiguangschedule.shared.generated.resources.alarm_option_none
import shiguangschedule.shared.generated.resources.alarm_option_on_time
import shiguangschedule.shared.generated.resources.dialog_title_ics_export_settings
import shiguangschedule.shared.generated.resources.dialog_title_select_export_table
import shiguangschedule.shared.generated.resources.dialog_title_select_import_table
import shiguangschedule.shared.generated.resources.label_select_alarm_time

/**
 * 闹铃提醒时间选项的本地化内部封装模型。
 */
private data class LocalizedAlarmOption(
    val value: Int?,
    private val displayString: String
) {
    override fun toString(): String = displayString
}

/**
 * 闹铃提前分钟数选择器组件。
 * 支持“无”、“准时”以及 1 到 60 分钟的选择。
 */
@Composable
fun AlarmMinutesPicker(
    modifier: Modifier = Modifier,
    initialValue: Int? = 15,
    onValueSelected: (Int?) -> Unit,
    itemHeight: Dp
) {
    val alarmOptionNone = stringResource(Res.string.alarm_option_none)
    val alarmOptionOnTime = stringResource(Res.string.alarm_option_on_time)

    val localizedOptions = remember(alarmOptionNone, alarmOptionOnTime) {
        buildList {
            add(LocalizedAlarmOption(null, alarmOptionNone))
            add(LocalizedAlarmOption(0, alarmOptionOnTime))
            for (i in 1..60) {
                add(LocalizedAlarmOption(i, i.toString()))
            }
        }
    }

    val initialOption = remember(initialValue, localizedOptions) {
        localizedOptions.find { it.value == initialValue } ?: localizedOptions.find { it.value == 15 }!!
    }

    NativeNumberPicker(
        values = localizedOptions,
        selectedValue = initialOption,
        onValueChange = { onValueSelected(it.value) },
        modifier = modifier,
        itemHeight = itemHeight
    )
}

/**
 * ICS 日历导出配置弹窗。
 * 包含闹铃提前时间配置步骤，确认后引导用户选择具体要导出的课表。
 */
@Composable
fun IcsExportDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, Int?) -> Unit
) {
    var alarmMinutes by remember { mutableStateOf<Int?>(15) }
    var showTablePicker by remember { mutableStateOf(false) }

    if (!showTablePicker) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(Res.string.dialog_title_ics_export_settings)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(Res.string.label_select_alarm_time))
                    Spacer(modifier = Modifier.height(16.dp))
                    AlarmMinutesPicker(
                        modifier = Modifier.width(150.dp),
                        onValueSelected = { alarmMinutes = it },
                        itemHeight = 48.dp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTablePicker = true }) {
                    Text(stringResource(Res.string.action_next_step))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    } else {
        CourseTablePickerDialog(
            title = stringResource(Res.string.dialog_title_select_export_table),
            onDismissRequest = onDismissRequest,
            onTableSelected = { selectedTable ->
                onConfirm(selectedTable.id, alarmMinutes)
            }
        )
    }
}

/**
 * 课表转换相关弹窗的统一管理中心组件。
 * 根据当前的 UI 状态（ConversionUiState）动态分发渲染导入或导出各类格式的对应弹窗。
 */
@Composable
fun ConversionDialogOverlay(
    uiState: ConversionUiState,
    onDismiss: () -> Unit,
    onConfirmImport: (String) -> Unit,
    onConfirmExport: (String, Int?) -> Unit
) {
    if (uiState.showImportTableDialog) {
        CourseTablePickerDialog(
            title = stringResource(Res.string.dialog_title_select_import_table),
            onDismissRequest = onDismiss,
            onTableSelected = { onConfirmImport(it.id) }
        )
    }

    if (uiState.showExportTableDialog) {
        when (uiState.exportType) {
            ExportType.JSON -> {
                CourseTablePickerDialog(
                    title = stringResource(Res.string.dialog_title_select_export_table),
                    onDismissRequest = onDismiss,
                    onTableSelected = { onConfirmExport(it.id, null) }
                )
            }
            ExportType.ICS -> {
                IcsExportDialog(
                    onDismissRequest = onDismiss,
                    onConfirm = { tableId, alarmMinutes ->
                        onConfirmExport(tableId, alarmMinutes)
                    }
                )
            }
            else -> {}
        }
    }
}