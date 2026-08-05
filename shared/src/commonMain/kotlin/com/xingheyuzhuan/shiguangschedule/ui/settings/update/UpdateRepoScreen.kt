package com.xingheyuzhuan.shiguangschedule.ui.settings.update

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.data.model.RepoType
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateRepoScreen(
    onBack: () -> Unit,
    viewModel: UpdateRepoViewModel = koinViewModel()
) {
    // 观察 ViewModel 的 uiState
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.title_update_repo_screen)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.a11y_back)
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
            // 仓库选择与操作卡片
            RepoSelectionCard(
                repoList = uiState.repoList,
                selectedRepo = uiState.selectedRepo,
                currentUrl = uiState.currentEditableUrl,
                currentBranch = uiState.currentEditableBranch,
                currentUsername = uiState.currentEditableUsername,
                currentPassword = uiState.currentEditablePassword,
                isUpdating = uiState.isUpdating,
                isDeveloperModeEnabled = uiState.isDeveloperModeEnabled,
                onRepoSelected = { repo -> viewModel.selectRepository(repo) },
                onUrlChanged = { url -> viewModel.updateCurrentUrl(url) },
                onBranchChanged = { branch -> viewModel.updateCurrentBranch(branch) },
                onUsernameChanged = { username -> viewModel.updateCurrentUsername(username) },
                onPasswordChanged = { password -> viewModel.updateCurrentPassword(password) },
                onUpdateClicked = { viewModel.startUpdate() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 日志显示卡片
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
    currentUsername: String,
    currentPassword: String,
    isUpdating: Boolean,
    isDeveloperModeEnabled: Boolean,
    onRepoSelected: (RepositoryInfo) -> Unit,
    onUrlChanged: (String) -> Unit,
    onBranchChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onUpdateClicked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // 统一定义普通 TextField 的颜色方案
    val commonTextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    // 统一定义下拉框 TextField 的颜色方案
    val dropdownTextFieldColors = ExposedDropdownMenuDefaults.textFieldColors(
        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = Color.Transparent,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.label_select_repo),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedRepo?.name ?: stringResource(Res.string.text_select_repo_hint),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                        .fillMaxWidth(),
                    colors = dropdownTextFieldColors,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    val displayRepos = repoList.filter { repo ->
                        if (!isDeveloperModeEnabled) {
                            repo.repoType != RepoType.CUSTOM && repo.repoType != RepoType.PRIVATE_REPO
                        } else {
                            true
                        }
                    }

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

            RepoEditOptions(
                selectedRepo = selectedRepo,
                currentUrl = currentUrl,
                currentBranch = currentBranch,
                onUrlChanged = onUrlChanged,
                onBranchChanged = onBranchChanged,
                currentUsername = currentUsername,
                currentPassword = currentPassword,
                onUsernameChanged = onUsernameChanged,
                onPasswordChanged = onPasswordChanged,
                textFieldColors = commonTextFieldColors
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onUpdateClicked,
                enabled = !isUpdating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isUpdating) {
                        stringResource(Res.string.action_updating)
                    } else {
                        stringResource(Res.string.action_update)
                    }
                )
            }
            AnimatedVisibility(visible = isUpdating) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                }
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
    currentUsername: String,
    currentPassword: String,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    textFieldColors: TextFieldColors
) {
    // 只有在仓库被选中且可编辑时才显示编辑框
    if (selectedRepo?.editable == true) {

        Spacer(modifier = Modifier.height(16.dp))

        // URL 编辑框 (适用于 CUSTOM, PRIVATE_REPO)
        TextField(
            value = currentUrl,
            onValueChange = onUrlChanged,
            label = { Text(stringResource(Res.string.label_repo_url)) },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Branch 编辑框 (适用于 CUSTOM, PRIVATE_REPO)
        TextField(
            value = currentBranch,
            onValueChange = onBranchChanged,
            label = { Text(stringResource(Res.string.label_repo_branch)) },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors
        )

        // 实现私有仓库的凭证输入
        if (selectedRepo.repoType == RepoType.PRIVATE_REPO) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.label_private_repo_credentials),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 用户名输入框
            TextField(
                value = currentUsername,
                onValueChange = onUsernameChanged,
                label = { Text(stringResource(Res.string.label_username_or_token_key)) },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = currentPassword,
                onValueChange = onPasswordChanged,
                label = { Text(stringResource(Res.string.label_password_or_token_value)) },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )
        }
    }
}

@Composable
fun LogDisplayCard(logs: String) {
    val scrollState = rememberScrollState()

    LaunchedEffect(logs) {
        if (logs.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ){
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.title_update_log),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            SelectionContainer {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ){
                    Text(
                        text = logs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .verticalScroll(scrollState),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}