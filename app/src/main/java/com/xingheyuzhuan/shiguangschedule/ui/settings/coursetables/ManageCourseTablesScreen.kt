package com.xingheyuzhuan.shiguangschedule.ui.settings.coursetables

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCourseTablesScreen(
    navController: NavHostController,
    viewModel: ManageCourseTablesViewModel = viewModel(
        factory = ManageCourseTablesViewModel.provideFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()

    var showAddTableDialog by remember { mutableStateOf(false) }
    var newTableName by remember { mutableStateOf("") }

    var showEditTableDialog by remember { mutableStateOf(false) }
    var editingTableInfo by remember { mutableStateOf<CourseTable?>(null) }
    var editedTableName by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var tableToDelete by remember { mutableStateOf<CourseTable?>(null) }

    val titleManageTables = stringResource(R.string.title_manage_course_tables)
    val a11yBack = stringResource(R.string.a11y_back)
    val a11yAddNewTable = stringResource(R.string.a11y_add_new_table)
    val textNoTablesHint = stringResource(R.string.text_no_tables_hint)
    val dialogTitleAddTable = stringResource(R.string.dialog_title_add_table)
    val labelTableName = stringResource(R.string.label_table_name)
    val actionAdd = stringResource(R.string.action_add)
    val actionCancel = stringResource(R.string.action_cancel)
    val toastNameEmpty = stringResource(R.string.toast_name_empty)
    val toastSwitchSuccess = stringResource(R.string.toast_switch_table_success)
    val toastAddSuccess = stringResource(R.string.toast_add_table_success)
    val dialogTitleEditTable = stringResource(R.string.dialog_title_edit_table)
    val a11ySave = stringResource(R.string.a11y_save)
    val toastEditSuccess = stringResource(R.string.toast_edit_table_success)
    val dialogTitleConfirmDelete = stringResource(R.string.dialog_title_confirm_delete)
    val dialogTextConfirmDelete = stringResource(R.string.dialog_text_confirm_delete)
    val actionDelete = stringResource(R.string.a11y_delete) // 复用 a11y_delete 作为按钮文本
    val toastDeleteSuccess = stringResource(R.string.toast_delete_table_success)
    val toastDeleteLastFailed = stringResource(R.string.toast_delete_last_table_failed)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleManageTables) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = a11yBack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = textNoTablesHint, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.courseTables) { tableInfo ->
                        val isSelected = tableInfo.id == uiState.currentActiveTableId
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
                                Toast.makeText(context, toastSwitchSuccess.format(it.name), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        if (showAddTableDialog) {
            AlertDialog(
                onDismissRequest = { showAddTableDialog = false; newTableName = "" },
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
                    Button(
                        onClick = {
                            if (newTableName.isNotBlank()) {
                                // 直接将字符串名称传递给 ViewModel
                                viewModel.createNewCourseTable(newTableName)
                                Toast.makeText(context, toastAddSuccess.format(newTableName), Toast.LENGTH_SHORT).show()
                                showAddTableDialog = false
                                newTableName = ""
                            } else {
                                Toast.makeText(context, toastNameEmpty, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(actionAdd)
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddTableDialog = false; newTableName = "" }) {
                        Text(actionCancel)
                    }
                }
            )
        }

        if (showEditTableDialog && editingTableInfo != null) {
            AlertDialog(
                onDismissRequest = { showEditTableDialog = false; editingTableInfo = null; editedTableName = "" },
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
                    Button(
                        onClick = {
                            if (editedTableName.isNotBlank()) {
                                editingTableInfo?.let { tableToEdit ->
                                    val updatedTable = tableToEdit.copy(name = editedTableName)
                                    viewModel.updateCourseTable(updatedTable)
                                    Toast.makeText(context, toastEditSuccess, Toast.LENGTH_SHORT).show()
                                    showEditTableDialog = false
                                    editingTableInfo = null
                                    editedTableName = ""
                                }
                            } else {
                                Toast.makeText(context, toastNameEmpty, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(a11ySave)
                    }
                },
                dismissButton = {
                    Button(onClick = { showEditTableDialog = false; editingTableInfo = null; editedTableName = "" }) {
                        Text(actionCancel)
                    }
                }
            )
        }

        if (showDeleteConfirmDialog && tableToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false; tableToDelete = null },
                title = { Text(dialogTitleConfirmDelete) },
                text = { Text(dialogTextConfirmDelete.format(tableToDelete?.name ?: "")) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (uiState.courseTables.size > 1) { // 使用 ViewModel 的数据进行检查
                                tableToDelete?.let {
                                    viewModel.deleteCourseTable(it)
                                    Toast.makeText(context, toastDeleteSuccess.format(it.name), Toast.LENGTH_SHORT).show()
                                }
                                showDeleteConfirmDialog = false
                                tableToDelete = null
                            } else {
                                Toast.makeText(context, toastDeleteLastFailed, Toast.LENGTH_SHORT).show()
                                showDeleteConfirmDialog = false
                                tableToDelete = null
                            }
                        }
                    ) {
                        Text(actionDelete)
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteConfirmDialog = false; tableToDelete = null }) {
                        Text(actionCancel)
                    }
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
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val a11yCurrentTable = stringResource(R.string.a11y_current_table)
    val a11yEdit = stringResource(R.string.a11y_edit)
    val a11yDelete = stringResource(R.string.a11y_delete)
    val idPrefix = stringResource(R.string.course_table_id_prefix)
    val createdAtPrefix = stringResource(R.string.course_table_created_at_prefix)

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
                Text(text = idPrefix.format(tableInfo.id.take(8) + "..."), style = MaterialTheme.typography.bodySmall)
                Text(
                    text = createdAtPrefix.format(dateFormatter.format(Date(tableInfo.createdAt))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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