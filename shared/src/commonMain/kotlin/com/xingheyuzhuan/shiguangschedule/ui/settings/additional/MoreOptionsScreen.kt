package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.tool.UpdateChecker
import com.xingheyuzhuan.shiguangschedule.tool.UpdatePlatform
import com.xingheyuzhuan.shiguangschedule.tool.UpdateStatus
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.*

private const val GITHUB_REPO_URL = "https://github.com/XingHeYuZhuan/shiguangschedule"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    updateChecker: UpdateChecker = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    // 从 Koin 动态获取注入的版本号
    val appVersionName: String = koinInject(named("AppVersionName"))

    // 状态观察
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDeveloperModeEnabled = uiState.appSettings.developerModeEnabled

    // 更新逻辑相关状态
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var selectedPlatform by remember { mutableStateOf(UpdatePlatform.GITEE) }

    // 弹窗可见性控制
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var showStartScreenDialog by remember { mutableStateOf(false) }

    // 逻辑：执行更新检查
    val startUpdateCheck: (UpdatePlatform) -> Unit = { platform ->
        selectedPlatform = platform
        showChannelDialog = false
        updateStatus = UpdateStatus.Checking
        showUpdateDialog = true
        coroutineScope.launch {
            updateStatus = updateChecker.checkUpdate(platform, appVersionName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.title_more_options)) },
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
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用信息头部
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DynamicAppIconHeader(
                    isDeveloperModeEnabled = isDeveloperModeEnabled,
                    onTriggerDeveloperMode = { viewModel.onDeveloperModeChanged(true) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = stringResource(Res.string.label_version_prefix, appVersionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 设置列表卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // 开发者模式设置项
                    DeveloperModeSettingItem(
                        isDeveloperModeEnabled = isDeveloperModeEnabled,
                        onDeveloperModeChanged = { viewModel.onDeveloperModeChanged(it) }
                    )

                    // 检查更新
                    SettingListItem(
                        icon = Icons.Default.Update,
                        title = stringResource(Res.string.item_check_software_update),
                        onClick = { showChannelDialog = true }
                    )

                    // 语言切换 (导航至独立页面)
                    SettingListItem(
                        icon = Icons.Default.Language,
                        title = stringResource(Res.string.item_language_settings),
                        onClick = { onNavigate(Destination.LanguageSettings) }
                    )

                    // 主题设置
                    SettingListItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(Res.string.theme_settings_title),
                        onClick = { onNavigate(Destination.ThemeSettings) }
                    )

                    // 启动页面设置
                    SettingListItem(
                        icon = Icons.Default.Home,
                        title = stringResource(Res.string.item_start_screen_settings),
                        onClick = { showStartScreenDialog = true },
                        trailingContent = {
                            Text(
                                text = stringResource(uiState.appSettings.startScreen.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    // GitHub 仓库
                    SettingListItem(
                        icon = Icons.Default.Code,
                        title = stringResource(Res.string.item_github_repo),
                        onClick = { uriHandler.openUri(GITHUB_REPO_URL) }
                    )

                    // 开源许可证
                    SettingListItem(
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        title = stringResource(Res.string.item_open_source_licenses),
                        onClick = { onNavigate(Destination.OpenSourceLicenses) }
                    )

                    // 更新适配仓库
                    SettingListItem(
                        icon = Icons.Default.Update,
                        title = stringResource(Res.string.item_update_repo),
                        onClick = { onNavigate(Destination.UpdateRepo) }
                    )

                    // 贡献者
                    SettingListItem(
                        icon = Icons.Default.PeopleAlt,
                        title = stringResource(Res.string.item_contributors),
                        onClick = { onNavigate(Destination.ContributionList) },
                        showDivider = false
                    )

                    // 鸣谢内容
                    AcknowledgmentContent()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- 弹窗逻辑 ---

    // 启动页切换弹窗
    StartScreenSelectionDialog(
        showDialog = showStartScreenDialog,
        currentSelected = uiState.appSettings.startScreen,
        onDismiss = { showStartScreenDialog = false },
        onConfirm = {
            viewModel.onStartScreenChanged(it)
            showStartScreenDialog = false
        }
    )

    // 检查更新结果弹窗
    UpdateResultDialog(
        showDialog = showUpdateDialog,
        updateStatus = updateStatus,
        onDismiss = {
            showUpdateDialog = false
            if (updateStatus !is UpdateStatus.Found) updateStatus = UpdateStatus.Idle
        },
        onDownloadClick = { targetUrl ->
            updateChecker.launchUpdate(targetUrl)
        }
    )

    // 更新渠道选择弹窗
    ChannelSelectionDialog(
        showDialog = showChannelDialog,
        currentSelected = selectedPlatform,
        onDismiss = { showChannelDialog = false },
        onConfirm = startUpdateCheck
    )
}