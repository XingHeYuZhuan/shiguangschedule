package com.xingheyuzhuan.shiguangschedule.ui.settings.contribution

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.data.model.ContributionList

// 为方便 UI 代码，定义 Contributor 别名
private typealias Contributor = ContributionList.Contributor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionScreen(
    navController: NavHostController,
    viewModel: ContributionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("贡献列表") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上一页")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 选项卡栏
            ContributionTabs(selectedTabIndex) { index ->
                viewModel.selectTab(index)
            }

            // 主内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                when (val state = uiState) {
                    ContributionUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    is ContributionUiState.Error -> {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            ErrorMessage(state.message) { viewModel.loadContributions() }
                        }
                    }

                    is ContributionUiState.Success -> {
                        // 逻辑：0 = 教务适配 (jiaowuadapter), 1 = 软件开发 (appDev)
                        val listToShow = when (selectedTabIndex) {
                            0 -> state.data.jiaowuadapter
                            1 -> state.data.appDev
                            else -> emptyList()
                        }
                        ContributorListContent(listToShow, context)
                    }
                }
            }
        }
    }
}

// 选项卡 Composable
@Composable
fun ContributionTabs(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("教务适配", "软件开发")

    TabRow(selectedTabIndex = selectedTabIndex) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
}

// 列表内容 Composable
@Composable
fun ContributorListContent(list: List<Contributor>, context: Context) {
    if (list.isEmpty()) {
        Text("当前类别暂无贡献者信息。", modifier = Modifier.padding(top = 16.dp))
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(list, key = { it.url }) { contributor ->
            ContributorCard(contributor, context)
        }
    }
}

// 贡献者卡片 Composable
@Composable
fun ContributorCard(contributor: Contributor, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                val uri = contributor.url.toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fullAssetPath = "contributors_data/${contributor.avatar}"

            AsyncImage(
                model = "file:///android_asset/$fullAssetPath",
                contentDescription = "贡献者头像: ${contributor.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // 名称
            Text(
                text = contributor.name,
                style = MaterialTheme.typography.titleMedium
            )

            // 在卡片右侧展示 URL 提示
            Spacer(Modifier.weight(1f))
            Text(
                text = "GitHub",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// 辅助 Composable: 错误信息
@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "加载失败: $message", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}