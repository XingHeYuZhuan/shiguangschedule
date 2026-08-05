package com.xingheyuzhuan.shiguangschedule.ui.settings.coursetables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.*
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCourseTablesScreen(
    onBack: () -> Unit,
    viewModel: ManageCourseTablesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- 对话框状态管理 ---
    var showAddTableDialog by remember { mutableStateOf(false) }
    var newTableName by remember { mutableStateOf("") }

    var showEditTableDialog by remember { mutableStateOf(false) }
    var editingTableInfo by remember { mutableStateOf<CourseTable?>(null) }
    var editedTableName by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var tableToDelete by remember { mutableStateOf<CourseTable?>(null) }

    // --- 资源字符串 ---
    val titleManageTables = stringResource(Res.string.title_manage_course_tables)
    val a11yBack = stringResource(Res.string.a11y_back)
    val a11yAddNewTable = stringResource(Res.string.a11y_add_new_table)
    val textNoTablesHint = stringResource(Res.string.text_no_tables_hint)
    val dialogTitleAddTable = stringResource(Res.string.dialog_title_add_table)
    val labelTableName = stringResource(Res.string.label_table_name)
    val actionAdd = stringResource(Res.string.action_add)
    val actionCancel = stringResource(Res.string.action_cancel)
    val toastNameEmpty = stringResource(Res.string.toast_name_empty)
    val toastEditSuccess = stringResource(Res.string.toast_edit_table_success)
    val dialogTitleEditTable = stringResource(Res.string.dialog_title_edit_table)
    val a11ySave = stringResource(Res.string.a11y_save)
    val dialogTitleConfirmDelete = stringResource(Res.string.confirm_delete)
    val actionDelete = stringResource(Res.string.a11y_delete)
    val toastDeleteLastFailed = stringResource(Res.string.toast_delete_last_table_failed)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleManageTables) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = a11yBack)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTableDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = a11yAddNewTable)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.courseTables.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = textNoTablesHint, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.courseTables, key = { it.id }) { tableInfo ->
                        val isSelected = tableInfo.id == uiState.currentActiveTableId
                        val switchSuccessMsg = stringResource(Res.string.toast_switch_table_success, tableInfo.name)

                        CourseTableCard(
                            tableInfo = tableInfo,
                            isSelected = isSelected,
                            onDeleteClick = {
                                tableToDelete = it
                                showDeleteConfirmDialog = true
                            },
                            onEditClick = {
                                editingTableInfo = it
                                editedTableName = it.name
                                showEditTableDialog = true
                            },
                            onCardClick = {
                                viewModel.switchCourseTable(it.id)
                                ToastManager.show(switchSuccessMsg)
                            }
                        )
                    }
                }
            }
        }

        // --- Add Dialog ---
        if (showAddTableDialog) {
            val addSuccessMsg = stringResource(Res.string.toast_add_table_success, newTableName)
            AlertDialog(
                onDismissRequest = {
                    showAddTableDialog = false
                    newTableName = ""
                },
                title = { Text(dialogTitleAddTable) },
                text = {
                    OutlinedTextField(
                        value = newTableName,
                        onValueChange = { newTableName = it },
                        label = { Text(labelTableName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newTableName.isNotBlank()) {
                            viewModel.createNewCourseTable(newTableName)
                            ToastManager.show(addSuccessMsg)
                            showAddTableDialog = false
                            newTableName = ""
                        } else {
                            ToastManager.show(toastNameEmpty)
                        }
                    }) { Text(actionAdd) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddTableDialog = false
                        newTableName = ""
                    }) { Text(actionCancel) }
                }
            )
        }

        // --- Edit Dialog ---
        if (showEditTableDialog && editingTableInfo != null) {
            AlertDialog(
                onDismissRequest = {
                    showEditTableDialog = false
                    editingTableInfo = null
                    editedTableName = ""
                },
                title = { Text(dialogTitleEditTable) },
                text = {
                    OutlinedTextField(
                        value = editedTableName,
                        onValueChange = { editedTableName = it },
                        label = { Text(labelTableName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editedTableName.isNotBlank()) {
                            editingTableInfo?.let { tableToEdit ->
                                viewModel.updateCourseTable(tableToEdit.copy(name = editedTableName))
                                ToastManager.show(toastEditSuccess)
                                showEditTableDialog = false
                                editingTableInfo = null
                                editedTableName = ""
                            }
                        } else {
                            ToastManager.show(toastNameEmpty)
                        }
                    }) { Text(a11ySave) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEditTableDialog = false
                        editingTableInfo = null
                        editedTableName = ""
                    }) { Text(actionCancel) }
                }
            )
        }

        // --- Delete Dialog ---
        if (showDeleteConfirmDialog && tableToDelete != null) {
            val confirmDeleteText = stringResource(Res.string.dialog_text_confirm_delete, tableToDelete?.name ?: "")
            val deleteSuccessMsg = stringResource(Res.string.toast_delete_table_success, tableToDelete?.name ?: "")

            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    tableToDelete = null
                },
                title = { Text(dialogTitleConfirmDelete) },
                text = { Text(confirmDeleteText) },
                confirmButton = {
                    TextButton(onClick = {
                        if (uiState.courseTables.size > 1) {
                            tableToDelete?.let {
                                viewModel.deleteCourseTable(it)
                                ToastManager.show(deleteSuccessMsg)
                            }
                            showDeleteConfirmDialog = false
                            tableToDelete = null
                        } else {
                            ToastManager.show(toastDeleteLastFailed)
                            showDeleteConfirmDialog = false
                            tableToDelete = null
                        }
                    }) {
                        Text(actionDelete, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirmDialog = false
                        tableToDelete = null
                    }) { Text(actionCancel) }
                }
            )
        }
    }
}

@Composable
fun CourseTableCard(
    tableInfo: CourseTable,
    isSelected: Boolean,
    onDeleteClick: (CourseTable) -> Unit,
    onEditClick: (CourseTable) -> Unit,
    onCardClick: (CourseTable) -> Unit
) {
    val a11yCurrentTable = stringResource(Res.string.a11y_current_table)
    val a11yEdit = stringResource(Res.string.a11y_edit)
    val a11yDelete = stringResource(Res.string.a11y_delete)

    val formattedId = tableInfo.id.take(8) + "..."
    val formattedDate = formatTimestamp(tableInfo.createdAt)

    val idText = stringResource(Res.string.course_table_id_prefix, formattedId)
    val createdAtText = stringResource(Res.string.course_table_created_at_prefix, formattedDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(tableInfo) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tableInfo.name, style = MaterialTheme.typography.titleMedium)
                Text(text = idText, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = createdAtText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = a11yCurrentTable,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                IconButton(onClick = { onEditClick(tableInfo) }) {
                    Icon(Icons.Default.Edit, contentDescription = a11yEdit)
                }
                IconButton(onClick = { onDeleteClick(tableInfo) }) {
                    Icon(Icons.Default.Delete, contentDescription = a11yDelete)
                }
            }
        }
    }
}

/**
 * 跨平台时间戳格式化 (yyyy-MM-dd HH:mm)
 */
private fun formatTimestamp(timestamp: Long): String {
    val localDateTime = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val year = localDateTime.year
    val month = localDateTime.month.number.toString().padStart(2, '0')
    val day = localDateTime.day.toString().padStart(2, '0')
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')

    return "$year-$month-$day $hour:$minute"
}