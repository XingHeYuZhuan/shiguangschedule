package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.Screen
import com.xingheyuzhuan.shiguangschedule.tool.UpdateChecker
import com.xingheyuzhuan.shiguangschedule.tool.UpdateStatus
import com.xingheyuzhuan.shiguangschedule.tool.UpdateChecker.Companion.UPDATE_CHANNELS
import com.xingheyuzhuan.shiguangschedule.ui.settings.SectionTitle
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingItemCard
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsSectionContainer
import kotlinx.coroutines.launch

private const val GITHUB_REPO_URL = "https://github.com/XingHeYuZhuan/shiguangschedule"

@Composable
private fun MoreOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.a11y_navigate),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AcknowledgmentContent(modifier: Modifier = Modifier) {
    val a11yAcknowledgment = stringResource(R.string.a11y_acknowledgment)
    val labelSpecialThanks = stringResource(R.string.label_special_thanks)
    val textAcknowledgmentBody = stringResource(R.string.text_acknowledgment_body)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = a11yAcknowledgment,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = labelSpecialThanks,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = textAcknowledgmentBody,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/**
 * 更新渠道选择弹窗
 */
@Composable
private fun ChannelSelectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    currentSelectedUrl: String,
    onChannelSelected: (String) -> Unit
) {
    if (!showDialog) return

    val dialogTitle = stringResource(R.string.dialog_select_update_channel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                UPDATE_CHANNELS.forEach { channel ->
                    val isSelected = channel.url == currentSelectedUrl

                    val onItemClick = {
                        onChannelSelected(channel.url)
                    }

                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onItemClick),

                        headlineContent = { Text(text = channel.title) },

                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),

                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = onItemClick,
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(currentSelectedUrl)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * 更新检查结果弹窗
 */
@Composable
private fun UpdateResultDialog(
    showDialog: Boolean,
    updateStatus: UpdateStatus,
    onDismiss: () -> Unit,
    onDownloadClick: (String) -> Unit
) {
    val context = LocalContext.current

    // 检查中和空闲状态不显示弹窗
    if (!showDialog || updateStatus is UpdateStatus.Checking || updateStatus is UpdateStatus.Idle) {
        if (showDialog && updateStatus is UpdateStatus.Checking) {
            // 如果在检查中，显示加载弹窗
            AlertDialog(
                onDismissRequest = { /* 检查中不允许关闭 */ },
                title = { Text(stringResource(R.string.dialog_checking_update)) },
                text = { Text(stringResource(R.string.tip_please_wait)) },
                confirmButton = {}
            )
        }
        return
    }

    val title: String
    val text: String
    val confirmButton: (@Composable () -> Unit)?

    when (updateStatus) {
        is UpdateStatus.Found -> {
            title = stringResource(R.string.dialog_new_version_found, updateStatus.flavorInfo.latestVersionName)
            text = updateStatus.flavorInfo.changelog
            confirmButton = {
                Button(onClick = { onDownloadClick(updateStatus.downloadUrl) }) {
                    Text(stringResource(R.string.btn_download_update))
                }
            }
        }
        is UpdateStatus.Latest -> {
            title = stringResource(R.string.dialog_current_version_latest)
            text = context.getString(R.string.label_version_prefix, updateStatus.versionName)
            confirmButton = null
        }
        is UpdateStatus.Error -> {
            title = stringResource(R.string.dialog_update_check_failed)
            text = stringResource(R.string.label_error_message, updateStatus.message)
            confirmButton = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // 使用 Column 结合 verticalScroll，确保内容可滚动
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 更新日志/错误信息
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { confirmButton?.invoke() },
        dismissButton = if (updateStatus is UpdateStatus.Found) {
            {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        } else {
            {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(navController: NavController) {

    val context = LocalContext.current
    val coroutineScope = LocalLifecycleOwner.current.lifecycleScope
    val scrollState = rememberScrollState()

    // 状态管理
    val checker = remember { UpdateChecker(context) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showChannelSelectionDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var selectedChannelUrl by remember {
        mutableStateOf(UpdateChecker.DEFAULT_PLATFORM_URL)
    }

    // 应用信息获取
    val defaultAppName = stringResource(R.string.default_app_name)
    val a11yAppIcon = stringResource(R.string.a11y_app_icon)

    val (appName, appVersion, appIconId) = remember(context) {
        val info = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        val name = info?.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: defaultAppName
        val version = info?.versionName ?: "N/A"
        val iconId = info?.applicationInfo?.icon ?: 0

        Triple(name, version, iconId)
    }

    // 开始更新检查的逻辑
    val startUpdateCheck: (String) -> Unit = { platformUrl ->
        updateStatus = UpdateStatus.Checking
        showUpdateDialog = true
        coroutineScope.launch {
            val result = checker.checkUpdate(platformUrl)
            updateStatus = result
        }
    }


    // 更新检查触发逻辑 (供列表项点击调用)
    val onCheckClick: () -> Unit = fun() {
        if (updateStatus is UpdateStatus.Checking) return

        // 重置 selectedChannelUrl 为默认值
        selectedChannelUrl = UpdateChecker.DEFAULT_PLATFORM_URL

        // 显示渠道选择弹窗
        showChannelSelectionDialog = true
    }

    // 处理下载点击
    val onDownloadClick: (String) -> Unit = { downloadUrl ->
        checker.launchExternalDownload(downloadUrl)
        showUpdateDialog = false
        updateStatus = UpdateStatus.Idle
    }

    // 处理弹窗关闭
    val onDismissDialog: () -> Unit = {
        showUpdateDialog = false
        if (updateStatus !is UpdateStatus.Found) {
            updateStatus = UpdateStatus.Idle
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.title_more_options))
                },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用信息区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (appIconId != 0) {
                    AsyncImage(
                        model = appIconId,
                        contentDescription = a11yAppIcon,
                        modifier = Modifier.size(128.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = a11yAppIcon,
                        modifier = Modifier.size(128.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.label_version_prefix, appVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 设置项列表区域
            SettingsSectionContainer {
                // 检查更新
                SettingItemCard(isFirst = true, isLast = false) {
                    MoreOptionItem(
                        icon = Icons.Default.Update,
                        title = stringResource(R.string.item_check_software_update),
                        onClick = onCheckClick
                    )
                }

                // 语言切换
                SettingItemCard(isFirst = false, isLast = false) {
                    MoreOptionItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.item_language_settings),
                        onClick = {
                            handleLanguageSettingClick(context) {
                                showLanguageDialog = true
                            }
                        }
                    )
                }

                // GitHub 仓库
                SettingItemCard(isFirst = false, isLast = false) {
                    MoreOptionItem(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.item_github_repo),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, GITHUB_REPO_URL.toUri())
                            context.startActivity(intent)
                        }
                    )
                }

                // 查看开源许可证
                SettingItemCard(isFirst = false, isLast = false) {
                    MoreOptionItem(
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        title = stringResource(R.string.item_open_source_licenses),
                        onClick = {
                            navController.navigate(Screen.OpenSourceLicenses.route)
                        }
                    )
                }

                // 更新教务适配仓库
                SettingItemCard(isFirst = false, isLast = false) {
                    MoreOptionItem(
                        icon = Icons.Default.Update,
                        title = stringResource(R.string.item_update_repo),
                        onClick = {
                            navController.navigate(Screen.UpdateRepo.route)
                        }
                    )
                }

                // 贡献者列表
                SettingItemCard(isFirst = false, isLast = true) {
                    MoreOptionItem(
                        icon = Icons.Default.PeopleAlt,
                        title = stringResource(R.string.item_contributors),
                        onClick = {
                            navController.navigate(Screen.ContributionList.route)
                        }
                    )
                }
            }

            AcknowledgmentContent()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 更新结果弹窗
    UpdateResultDialog(
        showDialog = showUpdateDialog,
        updateStatus = updateStatus,
        onDismiss = onDismissDialog,
        onDownloadClick = onDownloadClick
    )

    // 渠道选择弹窗
    ChannelSelectionDialog(
        showDialog = showChannelSelectionDialog,
        onDismiss = { showChannelSelectionDialog = false },
        onConfirm = startUpdateCheck,
        currentSelectedUrl = selectedChannelUrl,
        onChannelSelected = { url -> selectedChannelUrl = url }
    )

    // 语言选择对话框
    LanguageSelectionDialog(
        showDialog = showLanguageDialog,
        onDismiss = { showLanguageDialog = false }
    )
}