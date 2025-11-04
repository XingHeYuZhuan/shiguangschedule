package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.Screen

private const val GITHUB_REPO_URL = "https://github.com/XingHeYuZhuan/shiguangschedule"

@Composable
private fun SettingListItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = { Text(text = title) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.a11y_navigate)
            )
        }
    )
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            thickness = 0.5.dp,
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(navController: NavController) {

    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 应用图标
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

                // 2. 应用名称
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 3. 版本号
                Text(
                    text = stringResource(R.string.label_version_prefix, appVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // GitHub 仓库
                    SettingListItem(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.item_github_repo),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL))
                            context.startActivity(intent)
                        }
                    )

                    // 查看开源许可证
                    SettingListItem(
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        title = stringResource(R.string.item_open_source_licenses),
                        onClick = {
                            navController.navigate(Screen.OpenSourceLicenses.route)
                        }
                    )

                    // 更新教务适配仓库
                    SettingListItem(
                        icon = Icons.Default.Update,
                        title = stringResource(R.string.item_update_repo),
                        onClick = {
                            navController.navigate(Screen.UpdateRepo.route)
                        }
                    )
                    SettingListItem(
                        icon = Icons.Default.PeopleAlt,
                        title = stringResource(R.string.item_contributors),
                        onClick = {
                            navController.navigate(Screen.ContributionList.route)
                        },
                        showDivider = true
                    )
                    AcknowledgmentContent()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}