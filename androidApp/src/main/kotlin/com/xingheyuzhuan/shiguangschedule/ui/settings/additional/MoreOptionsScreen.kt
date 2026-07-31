package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.tool.UpdateChecker
import com.xingheyuzhuan.shiguangschedule.tool.UpdateStatus
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource

private const val GITHUB_REPO_URL = "https://github.com/XingHeYuZhuan/shiguangschedule"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = LocalLifecycleOwner.current.lifecycleScope
    val scrollState = rememberScrollState()

    // 状态观察
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 更新逻辑相关状态
    val checker = remember { UpdateChecker(context) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var selectedChannelUrl by remember { mutableStateOf(UpdateChecker.DEFAULT_PLATFORM_URL) }

    // 弹窗可见性控制
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStartScreenDialog by remember { mutableStateOf(false) }

    // 获取应用信息
    val defaultAppName = stringResource(R.string.default_app_name)

    val (appName, appVersion, appIconId) = remember(context, defaultAppName) {
        val info = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) { null }

        Triple(
            info?.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: defaultAppName,
            info?.versionName ?: "N/A",
            info?.applicationInfo?.icon ?: 0
        )
    }

    // 逻辑：执行更新检查
    val startUpdateCheck: (String) -> Unit = { platformUrl ->
        updateStatus = UpdateStatus.Checking
        showUpdateDialog = true
        coroutineScope.launch {
            updateStatus = checker.checkUpdate(platformUrl)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.title_more_options)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (appIconId != 0) {
                    AsyncImage(model = appIconId, contentDescription = null, modifier = Modifier.size(128.dp))
                } else {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(128.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text(text = stringResource(R.string.label_version_prefix, appVersion), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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


                    // 检查更新
                    SettingListItem(
                        icon = Icons.Default.Update,
                        title = stringResource(R.string.item_check_software_update),
                        onClick = { showChannelDialog = true }
                    )

                    // 语言切换
                    SettingListItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.item_language_settings),
                        onClick = { handleLanguageSettingClick(context) { showLanguageDialog = true } }
                    )

                    // 主题设置
                    SettingListItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.theme_settings_title),
                        onClick = { onNavigate(Destination.ThemeSettings) }
                    )

                    // 启动页面设置
                    SettingListItem(
                        icon = Icons.Default.Home,
                        title = stringResource(R.string.item_start_screen_settings),
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
                        title = stringResource(R.string.item_github_repo),
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_REPO_URL.toUri())) }
                    )

                    // 开源许可证
                    SettingListItem(
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        title = stringResource(R.string.item_open_source_licenses),
                        onClick = { onNavigate(Destination.OpenSourceLicenses) }
                    )

                    // 更新适配仓库
                    SettingListItem(
                        icon = Icons.Default.Update,
                        title = stringResource(R.string.item_update_repo),
                        onClick = { onNavigate(Destination.UpdateRepo) }
                    )

                    // 贡献者
                    SettingListItem(
                        icon = Icons.Default.PeopleAlt,
                        title = stringResource(R.string.item_contributors),
                        onClick = { onNavigate(Destination.ContributionList) },
                        showDivider = false
                    )

                    // 鸣谢内容 (来自 Components)
                    AcknowledgmentContent()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- 弹窗逻辑组件 (来自 Dialogs) ---

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

    // 检查更新结果
    UpdateResultDialog(
        showDialog = showUpdateDialog,
        updateStatus = updateStatus,
        onDismiss = {
            showUpdateDialog = false
            if (updateStatus !is UpdateStatus.Found) updateStatus = UpdateStatus.Idle
        },
        onDownloadClick = { checker.launchExternalDownload(it) }
    )

    // 更新渠道选择
    ChannelSelectionDialog(
        showDialog = showChannelDialog,
        onDismiss = { showChannelDialog = false },
        onConfirm = startUpdateCheck,
        currentSelectedUrl = selectedChannelUrl,
        onChannelSelected = { selectedChannelUrl = it }
    )

    // 语言选择
    LanguageSelectionDialog(
        showDialog = showLanguageDialog,
        onDismiss = { showLanguageDialog = false }
    )
}