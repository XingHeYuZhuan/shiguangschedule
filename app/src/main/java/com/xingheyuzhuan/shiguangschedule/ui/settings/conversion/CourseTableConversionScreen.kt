package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.R
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTableConversionScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: CourseTableConversionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 各项系统文字资源
    val snackbarCannotOpenFile = stringResource(R.string.snackbar_cannot_open_file)
    val snackbarFileSelectionCanceled = stringResource(R.string.snackbar_file_selection_canceled)
    val snackbarCannotSaveFile = stringResource(R.string.snackbar_cannot_save_file)
    val snackbarFileSaveCanceled = stringResource(R.string.snackbar_file_save_canceled)
    val snackbarFileCopyFailedForShare = stringResource(R.string.snackbar_file_copy_failed_for_share)
    val snackbarCalendarPermissionDenied = stringResource(R.string.error_sync_calendar_failed)

    var pendingImportTableId by remember { mutableStateOf<String?>(null) }
    var pendingExportJsonContent by remember { mutableStateOf<String?>(null) }
    var pendingExportIcsTableId by remember { mutableStateOf<String?>(null) }
    var pendingAlarmMinutes by remember { mutableStateOf<Int?>(null) }
    var showShareDialog by remember { mutableStateOf<Triple<Uri, String, String>?>(null) }

    val importLauncher = rememberLauncherForActivityResult(OpenJsonDocumentContract()) { uri ->
        val tableId = pendingImportTableId
        if (uri != null && tableId != null) {
            val inputStream: InputStream? = try { context.contentResolver.openInputStream(uri) } catch (e: Exception) { null }
            if (inputStream != null) viewModel.handleFileImport(tableId, inputStream)
            else coroutineScope.launch { snackbarHostState.showSnackbar(snackbarCannotOpenFile) }
        } else if (uri == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarFileSelectionCanceled) }
        }
        pendingImportTableId = null
    }

    val exportLauncher = rememberLauncherForActivityResult(CreateJsonDocumentContract()) { uri ->
        val jsonContent = pendingExportJsonContent
        val filename = "shiguangschedule_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.json"
        if (uri != null && jsonContent != null) {
            val outputStream: OutputStream? = try { context.contentResolver.openOutputStream(uri) } catch (e: Exception) { null }
            if (outputStream != null) {
                outputStream.bufferedWriter().use { it.write(jsonContent) }
                showShareDialog = Triple(uri, "application/json", filename)
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarCannotSaveFile) }
            }
        } else if (uri == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarFileSaveCanceled) }
        }
        pendingExportJsonContent = null
    }

    val icsExportLauncher = rememberLauncherForActivityResult(CreateIcsDocumentContract()) { uri ->
        val tableId = pendingExportIcsTableId
        val alarmMinutes = pendingAlarmMinutes
        val filename = "shiguangschedule_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.ics"
        if (uri != null && tableId != null) {
            val outputStream: OutputStream? = try { context.contentResolver.openOutputStream(uri) } catch (e: Exception) { null }
            if (outputStream != null) {
                viewModel.handleIcsExport(tableId, outputStream, alarmMinutes)
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

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) viewModel.onSyncToCalendarClick()
        else coroutineScope.launch { snackbarHostState.showSnackbar(snackbarCalendarPermissionDenied) }
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
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    exportLauncher.launch("shiguangschedule_$timestamp.json")
                }
                is ConversionEvent.LaunchExportIcsFileCreator -> {
                    pendingExportIcsTableId = event.tableId
                    pendingAlarmMinutes = event.alarmMinutes
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    icsExportLauncher.launch("shiguangschedule_$timestamp.ics")
                }
                is ConversionEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // 页面树结构中心
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_conversion)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.a11y_back)
                            )
                        }
                    }
                )
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
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

            // 类别一：文件转换
            Text(stringResource(R.string.section_file_conversion), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConversionRow(
                        title = stringResource(R.string.item_import_course_file),
                        desc = stringResource(R.string.desc_import_json),
                        onClick = { viewModel.onImportClick() }
                    )
                    HorizontalDivider()
                    ConversionRow(
                        title = stringResource(R.string.item_export_course_file),
                        desc = stringResource(R.string.desc_export_json_with_config),
                        onClick = { viewModel.onExportClick() }
                    )
                    HorizontalDivider()
                    ConversionRow(
                        title = stringResource(R.string.item_export_ics_file),
                        desc = stringResource(R.string.desc_export_ics_with_alarm),
                        onClick = { viewModel.onExportIcsClick() }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 类别二：教务导入
            Text(stringResource(R.string.section_school_import), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConversionRow(
                        title = stringResource(R.string.item_school_system_import),
                        desc = stringResource(R.string.desc_school_import_quick),
                        onClick = { onNavigate(Destination.SchoolSelectionListScreen) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 类别三：系统同步
            Text(stringResource(R.string.section_sync), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConversionRow(
                        title = stringResource(R.string.item_sync_to_system_calendar),
                        desc = stringResource(R.string.desc_sync_to_system_calendar),
                        onClick = {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_CALENDAR,
                                    android.Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        }
                    )
                    HorizontalDivider()

                    ConversionRow(
                        title = stringResource(R.string.item_backup_restore),
                        desc = stringResource(R.string.desc_backup_restore),
                        onClick = { onNavigate(Destination.BackupAndRestore) }
                    )
                }
            }
        }
    }

    // --- 独立分离出来的挂载弹窗部分 ---

    ConversionDialogOverlay(
        uiState = uiState,
        onDismiss = { viewModel.dismissDialog() },
        onConfirmImport = { viewModel.onImportTableSelected(it) },
        onConfirmExport = { id, mins -> viewModel.onExportTableSelected(id, mins) }
    )

    showShareDialog?.let { shareData ->
        ShareFileDialog(
            shareData = shareData,
            context = context,
            onDismiss = { showShareDialog = null },
            onCopyFailed = {
                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarFileCopyFailedForShare) }
            }
        )
    }
}

/**
 * 提取抽离的无障碍条目子组件，简化核心页面树
 */
@Composable
private fun ConversionRow(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = stringResource(R.string.a11y_details),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}