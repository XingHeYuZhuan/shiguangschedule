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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理课表") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                Icon(Icons.Default.Add, contentDescription = "添加新课表")
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
                    Text(text = "暂无课表，点击右下角按钮添加。", style = MaterialTheme.typography.bodyLarge)
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
                                Toast.makeText(context, "已切换到课表: ${it.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        if (showAddTableDialog) {
            AlertDialog(
                onDismissRequest = { showAddTableDialog = false; newTableName = "" },
                title = { Text("添加新课表") },
                text = {
                    OutlinedTextField(
                        value = newTableName,
                        onValueChange = { newTableName = it },
                        label = { Text("课表名称") },
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
                                Toast.makeText(context, "课表 '${newTableName}' 已添加", Toast.LENGTH_SHORT).show()
                                showAddTableDialog = false
                                newTableName = ""
                            } else {
                                Toast.makeText(context, "课表名称不能为空", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("添加")
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddTableDialog = false; newTableName = "" }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showEditTableDialog && editingTableInfo != null) {
            AlertDialog(
                onDismissRequest = { showEditTableDialog = false; editingTableInfo = null; editedTableName = "" },
                title = { Text("编辑课表名称") },
                text = {
                    OutlinedTextField(
                        value = editedTableName,
                        onValueChange = { editedTableName = it },
                        label = { Text("新课表名称") },
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
                                    Toast.makeText(context, "课表名称已更新", Toast.LENGTH_SHORT).show()
                                    showEditTableDialog = false
                                    editingTableInfo = null
                                    editedTableName = ""
                                }
                            } else {
                                Toast.makeText(context, "课表名称不能为空", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    Button(onClick = { showEditTableDialog = false; editingTableInfo = null; editedTableName = "" }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showDeleteConfirmDialog && tableToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false; tableToDelete = null },
                title = { Text("确认删除") },
                text = { Text("您确定要删除课表 '${tableToDelete?.name}' 吗？此操作无法撤销。") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (uiState.courseTables.size > 1) { // 使用 ViewModel 的数据进行检查
                                tableToDelete?.let {
                                    viewModel.deleteCourseTable(it)
                                    Toast.makeText(context, "${it.name} 已删除", Toast.LENGTH_SHORT).show()
                                }
                                showDeleteConfirmDialog = false
                                tableToDelete = null
                            } else {
                                Toast.makeText(context, "不能删除最后一个课表", Toast.LENGTH_SHORT).show()
                                showDeleteConfirmDialog = false
                                tableToDelete = null
                            }
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteConfirmDialog = false; tableToDelete = null }) {
                        Text("取消")
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
                Text(text = "ID: ${tableInfo.id.substring(0, 8)}...", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "创建于: ${dateFormatter.format(Date(tableInfo.createdAt))}",
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
                        contentDescription = "当前课表",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                IconButton(onClick = { onEditClick(tableInfo) }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = { onDeleteClick(tableInfo) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}