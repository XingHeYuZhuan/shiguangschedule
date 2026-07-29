package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker


class OpenJsonDocumentContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

class CreateJsonDocumentContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, input)
        }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

class CreateIcsDocumentContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/calendar"
            putExtra(Intent.EXTRA_TITLE, input)
        }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

// --- 内部数据模型 ---
private data class LocalizedAlarmOption(val value: Int?, private val displayString: String) {
    override fun toString(): String = displayString
}

@Composable
fun AlarmMinutesPicker(
    modifier: Modifier = Modifier,
    initialValue: Int? = 15,
    onValueSelected: (Int?) -> Unit,
    itemHeight: Dp
) {
    val alarmOptionNone = stringResource(R.string.alarm_option_none)
    val alarmOptionOnTime = stringResource(R.string.alarm_option_on_time)

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
            title = { Text(stringResource(R.string.dialog_title_ics_export_settings)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.label_select_alarm_time))
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
                    Text(stringResource(R.string.action_next_step))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    } else {
        CourseTablePickerDialog(
            title = stringResource(R.string.dialog_title_select_export_table),
            onDismissRequest = onDismissRequest,
            onTableSelected = { selectedTable ->
                onConfirm(selectedTable.id, alarmMinutes)
            }
        )
    }
}

/**
 * 统一弹窗管理器管理中心
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
            title = stringResource(R.string.dialog_title_select_import_table),
            onDismissRequest = onDismiss,
            onTableSelected = { onConfirmImport(it.id) }
        )
    }

    if (uiState.showExportTableDialog) {
        when (uiState.exportType) {
            ExportType.JSON -> {
                CourseTablePickerDialog(
                    title = stringResource(R.string.dialog_title_select_export_table),
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