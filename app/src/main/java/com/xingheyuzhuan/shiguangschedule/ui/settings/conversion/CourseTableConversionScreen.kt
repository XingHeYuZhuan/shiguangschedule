package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.Screen
import com.xingheyuzhuan.shiguangschedule.tool.shareFile
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 自定义文件选择器 Contract，用于导入，只允许选择 JSON 文件
class OpenJsonDocumentContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

// 自定义文件创建器 Contract，用于导出，接受文件名作为输入
class CreateJsonDocumentContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

// 自定义 ICS 文件创建器 Contract
class CreateIcsDocumentContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/calendar" // ICS 文件的 MIME 类型
            putExtra(Intent.EXTRA_TITLE, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

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
        onValueChange = { selectedOption ->
            onValueSelected(selectedOption.value)
        },
        modifier = modifier,
        itemHeight = itemHeight
    )
}

// ICS 导出对话框，用于选择提醒时间和课表
@Composable
fun IcsExportDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, Int?) -> Unit
) {
    var alarmMinutes by remember { mutableStateOf<Int?>(15) }
    var showTablePicker by remember { mutableStateOf(false) }

    val dialogTitleIcsExport = stringResource(R.string.dialog_title_ics_export_settings)
    val labelSelectAlarm = stringResource(R.string.label_select_alarm_time)
    val actionCancel = stringResource(R.string.action_cancel)
    val actionNextStep = stringResource(R.string.action_next_step)
    val dialogTitleSelectExportTable = stringResource(R.string.dialog_title_select_export_table)

    // 当 showTablePicker 为 false 时，显示第一个对话框（提醒时间选择）
    if (!showTablePicker) {
        Dialog(onDismissRequest = onDismissRequest) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dialogTitleIcsExport,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(labelSelectAlarm, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AlarmMinutesPicker(
                        modifier = Modifier.width(150.dp),
                        onValueSelected = { minutes -> alarmMinutes = minutes },
                        itemHeight = 48.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text(actionCancel)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showTablePicker = true }) {
                            Text(actionNextStep)
                        }
                    }
                }
            }
        }
    }

    // 当 showTablePicker 为 true 时，显示第二个对话框（课表选择）
    if (showTablePicker) {
        CourseTablePickerDialog(
            title = dialogTitleSelectExportTable,
            // 这里我们希望关闭课表选择器时，整个导出流程都结束
            onDismissRequest = onDismissRequest,
            onTableSelected = { selectedTable ->
                // 在回调中，同时传递课表ID和之前选择的提醒时间
                onConfirm(selectedTable.id, alarmMinutes)
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTableConversionScreen(
    navController: NavHostController,
    viewModel: CourseTableConversionViewModel = viewModel(factory = CourseTableConversionViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val snackbarCannotOpenFile = stringResource(R.string.snackbar_cannot_open_file)
    val snackbarFileSelectionCanceled = stringResource(R.string.snackbar_file_selection_canceled)
    val snackbarCannotSaveFile = stringResource(R.string.snackbar_cannot_save_file)
    val snackbarFileSaveCanceled = stringResource(R.string.snackbar_file_save_canceled)
    val snackbarFileCopyFailedForShare = stringResource(R.string.snackbar_file_copy_failed_for_share)

    val dialogTitleFileSaved = stringResource(R.string.dialog_title_file_saved)
    val dialogTextFileSavedSharePrompt = stringResource(R.string.dialog_text_file_saved_share_prompt)
    val actionShare = stringResource(R.string.action_share)
    val actionCancel = stringResource(R.string.action_cancel)

    var pendingImportTableId by remember { mutableStateOf<String?>(null) }
    var pendingExportJsonContent by remember { mutableStateOf<String?>(null) }
    var pendingExportIcsTableId by remember { mutableStateOf<String?>(null) }
    var pendingAlarmMinutes by remember { mutableStateOf<Int?>(null) }

    // 新增状态，用于显示分享弹窗。保存公共目录的Uri和原始文件名。
    var showShareDialog by remember { mutableStateOf<Triple<Uri, String, String>?>(null) }

    // 文件导入启动器
    val importLauncher = rememberLauncherForActivityResult(OpenJsonDocumentContract()) { uri: Uri? ->
        val tableId = pendingImportTableId
        if (uri != null && tableId != null) {
            val inputStream: InputStream? = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                null
            }
            if (inputStream != null) {
                viewModel.handleFileImport(tableId, inputStream)
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarCannotOpenFile) }
            }
        } else if (uri == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarFileSelectionCanceled) }
        }
        pendingImportTableId = null
    }

    // 文件导出启动器
    val exportLauncher = rememberLauncherForActivityResult(CreateJsonDocumentContract()) { uri: Uri? ->
        val jsonContent = pendingExportJsonContent
        val filename = "shiguangschedule_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.json"
        if (uri != null && jsonContent != null) {
            val outputStream: OutputStream? = try {
                context.contentResolver.openOutputStream(uri)
            } catch (e: Exception) {
                null
            }
            if (outputStream != null) {
                // 将文件内容写入
                outputStream.bufferedWriter().use { writer ->
                    writer.write(jsonContent)
                }
                // 在文件保存成功后，设置状态以显示分享弹窗。我们保存公共Uri和我们想要的原始文件名。
                showShareDialog = Triple(uri, "application/json", filename)
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarCannotSaveFile) }
            }
        } else if (uri == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarFileSaveCanceled) }
        }
        pendingExportJsonContent = null
    }

    // ICS 文件导出启动器
    val icsExportLauncher = rememberLauncherForActivityResult(CreateIcsDocumentContract()) { uri: Uri? ->
        val tableId = pendingExportIcsTableId
        val alarmMinutes = pendingAlarmMinutes
        val filename = "shiguangschedule_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.ics"
        if (uri != null && tableId != null) {
            val outputStream: OutputStream? = try {
                context.contentResolver.openOutputStream(uri)
            } catch (e: Exception) {
                null
            }
            if (outputStream != null) {
                viewModel.handleIcsExport(tableId, outputStream, alarmMinutes)
                // 在文件保存成功后，设置状态以显示分享弹窗。我们保存公共Uri和我们想要的原始文件名。
                showShareDialog = Triple(uri, "text/calendar", filename)
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarCannotSaveFile) }
            }
        } else if (uri == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarFileSaveCanceled) }
        }
        pendingExportIcsTableId = null
        pendingAlarmMinutes = null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ConversionEvent.LaunchImportFilePicker -> {
                    pendingImportTableId = event.tableId
                    importLauncher.launch(Unit)
                }
                is ConversionEvent.LaunchExportFileCreator -> {
                    pendingExportJsonContent = event.jsonContent
                    val now = LocalDateTime.now()
                    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    val timestamp = now.format(formatter)
                    val filename = "shiguangschedule_$timestamp.json"
                    exportLauncher.launch(filename)
                }
                is ConversionEvent.LaunchExportIcsFileCreator -> {
                    pendingExportIcsTableId = event.tableId
                    pendingAlarmMinutes = event.alarmMinutes
                    val now = LocalDateTime.now()
                    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    val timestamp = now.format(formatter)
                    val filename = "shiguangschedule_$timestamp.ics"
                    icsExportLauncher.launch(filename)
                }
                is ConversionEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_conversion)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.section_file_conversion), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { viewModel.onImportClick() })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.item_import_course_file), style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.desc_import_json),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = stringResource(R.string.a11y_import),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { viewModel.onExportClick() })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.item_export_course_file), style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.desc_export_json_with_config),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = stringResource(R.string.a11y_export),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isLoading, onClick = { viewModel.onExportIcsClick() })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.item_export_ics_file), style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.desc_export_ics_with_alarm),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = stringResource(R.string.a11y_export),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.section_school_import), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.SchoolSelectionListScreen.route) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.item_school_system_import),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.desc_school_import_quick),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }

    if (uiState.showImportTableDialog) {
        CourseTablePickerDialog(
            title = stringResource(R.string.dialog_title_select_import_table),
            onDismissRequest = { viewModel.dismissDialog() },
            onTableSelected = { selectedTable ->
                viewModel.onImportTableSelected(selectedTable.id)
            }
        )
    }

    if (uiState.showExportTableDialog) {
        when (uiState.exportType) {
            ExportType.JSON -> {
                CourseTablePickerDialog(
                    title = stringResource(R.string.dialog_title_select_export_table),
                    onDismissRequest = { viewModel.dismissDialog() },
                    onTableSelected = { selectedTable ->
                        viewModel.onExportTableSelected(selectedTable.id, null)
                    }
                )
            }
            ExportType.ICS -> {
                IcsExportDialog(
                    onDismissRequest = { viewModel.dismissDialog() },
                    onConfirm = { tableId, alarmMinutes ->
                        viewModel.onExportTableSelected(tableId, alarmMinutes)
                    }
                )
            }
            else -> {
            }
        }
    }

    if (showShareDialog != null) {
        AlertDialog(
            onDismissRequest = { showShareDialog = null },
            title = { Text(dialogTitleFileSaved) },
            text = { Text(dialogTextFileSavedSharePrompt) },
            confirmButton = {
                TextButton(onClick = {
                    val (publicUri, mimeType, defaultFilename) = showShareDialog!!

                    val userDefinedFilename = context.contentResolver.query(
                        publicUri,
                        arrayOf(OpenableColumns.DISPLAY_NAME), // 明确指定要查询的列名
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            // 检查索引是否有效
                            if (nameIndex >= 0) {
                                cursor.getString(nameIndex)
                            } else null
                        } else null
                    } ?: defaultFilename // 如果查询失败，回退到代码中的默认时间戳文件名

                    // 确保 share_temp 目录存在
                    val shareTempDir = File(context.cacheDir, "share_temp")
                    if (!shareTempDir.exists()) {
                        shareTempDir.mkdirs()
                    }

                    val tempFile = File(shareTempDir, userDefinedFilename)

                    try {
                        context.contentResolver.openInputStream(publicUri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        coroutineScope.launch { snackbarHostState.showSnackbar(snackbarFileCopyFailedForShare) }
                        showShareDialog = null
                        return@TextButton
                    }

                    // FileProvider 将会根据 tempFile 的名称来设置分享的文件名
                    val shareUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile
                    )

                    // 使用 FileProvider 的 Uri 来分享
                    shareFile(context, shareUri, mimeType)

                    showShareDialog = null
                }) {
                    Text(actionShare)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showShareDialog = null
                }) {
                    Text(actionCancel)
                }
            }
        )
    }
}