package com.xingheyuzhuan.shiguangschedule.ui.settings.update

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateRepoScreen(
    navController: NavController,
    // 使用工厂模式注入 ViewModel，并传入 Context
    viewModel: UpdateRepoViewModel = viewModel(
        factory = UpdateRepoViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    // 观察ViewModel的uiState
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "更新仓库") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 第一个矩形区域：仓库选择与操作
            RepoSelectionCard(
                repoList = uiState.repoList,
                selectedRepo = uiState.selectedRepo,
                isUpdating = uiState.isUpdating,
                onRepoSelected = { repo -> viewModel.selectRepository(repo) },
                onUpdateClicked = { viewModel.startUpdate() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 第二个矩形区域：日志显示
            LogDisplayCard(logs = uiState.logs)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSelectionCard(
    repoList: List<RepositoryInfo>,
    selectedRepo: RepositoryInfo?,
    isUpdating: Boolean,
    onRepoSelected: (RepositoryInfo) -> Unit,
    onUpdateClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "选择仓库",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = false, // 简化示例
                onExpandedChange = { }
            ) {
                TextField(
                    value = selectedRepo?.name ?: "请选择仓库",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) }
                )
                ExposedDropdownMenu(
                    expanded = false, // 简化示例
                    onDismissRequest = {}
                ) {
                    repoList.forEach { repo ->
                        DropdownMenuItem(
                            text = { Text(repo.name) },
                            onClick = { onRepoSelected(repo) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUpdateClicked,
                enabled = !isUpdating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isUpdating) "更新中..." else "更新")
            }
        }
    }
}

@Composable
fun LogDisplayCard(logs: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "更新日志",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 新增：使用 SelectionContainer 包裹文本以实现选择和复制功能
            SelectionContainer {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = logs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PreviewUpdateRepoScreen() {
    MaterialTheme {
        UpdateRepoScreen(navController = rememberNavController(), viewModel = PreviewUpdateRepoViewModel())
    }
}

// 预览专用的 ViewModel
class PreviewUpdateRepoViewModel : UpdateRepoViewModel(
    gitRepository = object : com.xingheyuzhuan.shiguangschedule.data.repository.GitRepository {
        override suspend fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit) {}
    },
    jsonInputStream = """[{"name":"官方仓库","type":"OFFICIAL","url":"","branch":"","editable":false}]""".byteInputStream()
)