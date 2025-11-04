package com.xingheyuzhuan.shiguangschedule.ui.settings.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.xingheyuzhuan.shiguangschedule.BuildConfig
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.RepoType
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateRepoScreen(
    navController: NavController,
    viewModel: UpdateRepoViewModel = viewModel(factory = UpdateRepoViewModelFactory)
) {
    // 观察ViewModel的uiState
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.title_update_repo_screen)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_back)
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
                .verticalScroll(rememberScrollState())
        ) {
            // 第一个矩形区域：仓库选择与操作
            RepoSelectionCard(
                repoList = uiState.repoList,
                selectedRepo = uiState.selectedRepo,
                currentUrl = uiState.currentEditableUrl,
                currentBranch = uiState.currentEditableBranch,
                currentUsername = uiState.currentEditableUsername,
                currentPassword = uiState.currentEditablePassword,
                isUpdating = uiState.isUpdating,
                onRepoSelected = { repo -> viewModel.selectRepository(repo) },
                onUrlChanged = { url -> viewModel.updateCurrentUrl(url) },
                onBranchChanged = { branch -> viewModel.updateCurrentBranch(branch) },
                onUsernameChanged = { username -> viewModel.updateCurrentUsername(username) },
                onPasswordChanged = { password -> viewModel.updateCurrentPassword(password) },
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
    currentUrl: String,
    currentBranch: String,
    // 新增凭证状态参数
    currentUsername: String,
    currentPassword: String,
    isUpdating: Boolean,
    onRepoSelected: (RepositoryInfo) -> Unit,
    onUrlChanged: (String) -> Unit,
    onBranchChanged: (String) -> Unit,
    // 新增凭证事件参数
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onUpdateClicked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.label_select_repo),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 仓库选择下拉菜单
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedRepo?.name ?: stringResource(R.string.text_select_repo_hint),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(
                        MenuAnchorType.PrimaryEditable,
                        true
                    ).fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)}
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    val displayRepos = repoList.filter { repo ->
                        if (BuildConfig.HIDE_CUSTOM_REPOS) {
                            repo.repoType != RepoType.CUSTOM && repo.repoType != RepoType.PRIVATE_REPO
                        } else {
                            true
                        }
                    }

                    // 遍历筛选后的列表
                    displayRepos.forEach { repo ->
                        DropdownMenuItem(
                            text = { Text(repo.name) },
                            onClick = {
                                onRepoSelected(repo)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // 仓库编辑选项
            RepoEditOptions(
                selectedRepo = selectedRepo,
                currentUrl = currentUrl,
                currentBranch = currentBranch,
                onUrlChanged = onUrlChanged,
                onBranchChanged = onBranchChanged,
                currentUsername = currentUsername,
                currentPassword = currentPassword,
                onUsernameChanged = onUsernameChanged,
                onPasswordChanged = onPasswordChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUpdateClicked,
                enabled = !isUpdating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isUpdating) {
                        stringResource(R.string.action_updating)
                    } else {
                        stringResource(R.string.action_update)
                    }
                )
            }
        }
    }
}

/**
 * 根据仓库类型和可编辑性显示编辑选项
 */
@Composable
fun RepoEditOptions(
    selectedRepo: RepositoryInfo?,
    currentUrl: String,
    currentBranch: String,
    onUrlChanged: (String) -> Unit,
    onBranchChanged: (String) -> Unit,
    // 新增凭证状态和事件参数
    currentUsername: String,
    currentPassword: String,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit
) {
    // 只有在仓库被选中且可编辑时才显示编辑框
    if (selectedRepo?.editable == true) {

        Spacer(modifier = Modifier.height(16.dp))

        // URL 编辑框 (适用于 CUSTOM, PRIVATE_REPO)
        TextField(
            value = currentUrl,
            onValueChange = onUrlChanged,
            label = { Text(stringResource(R.string.label_repo_url)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Branch 编辑框 (适用于 CUSTOM, PRIVATE_REPO)
        TextField(
            value = currentBranch,
            onValueChange = onBranchChanged,
            label = { Text(stringResource(R.string.label_repo_branch)) },
            modifier = Modifier.fillMaxWidth()
        )

        // 实现私有仓库的凭证输入
        if (selectedRepo.repoType == RepoType.PRIVATE_REPO) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.label_private_repo_credentials),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 用户名输入框
            TextField(
                value = currentUsername,
                onValueChange = onUsernameChanged,
                label = { Text(stringResource(R.string.label_username_or_token_key)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = currentPassword,
                onValueChange = onPasswordChanged,
                label = { Text(stringResource(R.string.label_password_or_token_value)) },
                modifier = Modifier.fillMaxWidth()
            )
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
                text = stringResource(R.string.title_update_log),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            SelectionContainer {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
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