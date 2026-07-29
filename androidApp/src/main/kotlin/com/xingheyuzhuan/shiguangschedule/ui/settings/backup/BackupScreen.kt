package com.xingheyuzhuan.shiguangschedule.ui.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.R
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 备份/恢复的目标媒介枚举
 */
enum class BackupTarget(val stringResId: Int) {
    WEBDAV(R.string.backup_target_webdav),
    LOCAL_ZIP(R.string.backup_target_local_zip)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showConfigDialog by remember { mutableStateOf(false) }
    var showBackupTargetDialog by remember { mutableStateOf(false) }
    var showRestoreTargetDialog by remember { mutableStateOf(false) }

    val streamOpenFailedMsg = stringResource(R.string.error_stream_open_failed)
    val webdavUnconfiguredMsg = stringResource(R.string.error_webdav_unconfigured)
    val opSuccessMsg = stringResource(R.string.toast_operation_success)
    val opFailedPrefix = stringResource(R.string.toast_operation_failed, "")

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        viewModel.exportToLocalZip(outputStream)
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(streamOpenFailedMsg)
                }
            }
        }
    }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        viewModel.importFromLocalZip(inputStream)
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(streamOpenFailedMsg)
                }
            }
        }
    }

    LaunchedEffect(state.testResult) {
        when (val result = state.testResult) {
            is TestResult.Error -> {
                snackbarHostState.showSnackbar(
                    message = opFailedPrefix + result.message,
                    duration = SnackbarDuration.Short
                )
            }
            is TestResult.Success -> {
                snackbarHostState.showSnackbar(
                    message = opSuccessMsg,
                    duration = SnackbarDuration.Short
                )
            }
            TestResult.Idle -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.item_backup_restore)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (state.isBusy || state.isTesting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            CardGroup(title = stringResource(R.string.section_data_maintenance)) {
                MenuActionItem(
                    title = stringResource(R.string.item_backup_data),
                    subtitle = stringResource(R.string.desc_backup_data),
                    icon = Icons.Default.Upload,
                    enabled = !state.isBusy,
                    onClick = { showBackupTargetDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                MenuActionItem(
                    title = stringResource(R.string.item_restore_data),
                    subtitle = stringResource(R.string.desc_restore_data),
                    icon = Icons.Default.Download,
                    enabled = !state.isBusy,
                    onClick = { showRestoreTargetDialog = true }
                )
            }

            CardGroup(title = stringResource(R.string.section_service_config)) {
                MenuActionItem(
                    title = stringResource(R.string.item_webdav_config),
                    subtitle = if (state.baseUrl.isBlank()) {
                        stringResource(R.string.desc_webdav_unconfigured)
                    } else {
                        stringResource(R.string.desc_webdav_connected, state.baseUrl)
                    },
                    icon = Icons.Default.Cloud,
                    onClick = { showConfigDialog = true }
                )
            }
        }
    }

    if (showConfigDialog) {
        WebDavConfigDialog(
            state = state,
            onDismiss = { showConfigDialog = false },
            onSave = { u, n, p, r ->
                viewModel.testWebDavConnection(u, n, p, r)
                showConfigDialog = false
            },
            onDisconnect = {
                viewModel.disconnectWebDav()
                showConfigDialog = false
            }
        )
    }

    if (showBackupTargetDialog) {
        TargetSelectionDialog(
            title = stringResource(R.string.dialog_title_backup_target),
            onDismiss = { showBackupTargetDialog = false },
            onTargetSelected = { target ->
                showBackupTargetDialog = false
                when (target) {
                    BackupTarget.WEBDAV -> {
                        if (state.baseUrl.isNotBlank()) {
                            viewModel.backupToWebDav()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(webdavUnconfiguredMsg) }
                        }
                    }
                    BackupTarget.LOCAL_ZIP -> {
                        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                        val defaultFileName = "Shiguang_Backup_$dateStr.zip"
                        exportZipLauncher.launch(defaultFileName)
                    }
                }
            }
        )
    }

    if (showRestoreTargetDialog) {
        TargetSelectionDialog(
            title = stringResource(R.string.dialog_title_restore_source),
            onDismiss = { showRestoreTargetDialog = false },
            onTargetSelected = { target ->
                showRestoreTargetDialog = false
                when (target) {
                    BackupTarget.WEBDAV -> {
                        if (state.baseUrl.isNotBlank()) {
                            viewModel.restoreFromWebDav()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(webdavUnconfiguredMsg) }
                        }
                    }
                    BackupTarget.LOCAL_ZIP -> {
                        importZipLauncher.launch(arrayOf("application/zip"))
                    }
                }
            }
        )
    }
}

@Composable
fun CardGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(PaddingValues(start = 4.dp))
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
fun MenuActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp)
    )
}

@Composable
fun TargetSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onTargetSelected: (BackupTarget) -> Unit
) {
    var selectedTarget by remember { mutableStateOf(BackupTarget.entries.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                BackupTarget.entries.forEach { target ->
                    val isSelected = target == selectedTarget

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTarget = target }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedTarget = target }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(target.stringResId),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onTargetSelected(selectedTarget) }
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        }
    )
}

@Composable
fun WebDavConfigDialog(
    state: BackupUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onDisconnect: () -> Unit
) {
    var inputUrl by remember { mutableStateOf(state.baseUrl) }
    var inputUsername by remember { mutableStateOf(state.username) }
    var inputPassword by remember { mutableStateOf("") }
    var inputRootPath by remember { mutableStateOf(state.rootPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_config_webdav)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text(stringResource(R.string.label_webdav_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputUsername,
                    onValueChange = { inputUsername = it },
                    label = { Text(stringResource(R.string.label_webdav_account)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = { inputPassword = it },
                    label = {
                        Text(
                            if (state.hasSavedPassword) stringResource(R.string.label_webdav_pwd_saved)
                            else stringResource(R.string.label_webdav_pwd_empty)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputRootPath,
                    onValueChange = { inputRootPath = it },
                    label = { Text(stringResource(R.string.label_webdav_path)) },
                    supportingText = { Text(stringResource(R.string.desc_webdav_path_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(inputUrl, inputUsername, inputPassword, inputRootPath) },
                enabled = !state.isTesting
            ) {
                Text(
                    if (state.isTesting) stringResource(R.string.title_loading)
                    else stringResource(R.string.action_confirm)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDisconnect()
                onDismiss()
            }) {
                Text(
                    text = stringResource(R.string.action_reset),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}