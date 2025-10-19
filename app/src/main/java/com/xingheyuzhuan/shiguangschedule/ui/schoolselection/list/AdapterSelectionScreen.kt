package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list

// 导入 Screen 路由
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import school_index.Adapter
import school_index.AdapterCategory

object WebViewNavigationHelper {
    fun createRoute(initialUrl: String?, assetJsPath: String?): String {
        val urlParam = Uri.encode(initialUrl ?: "about:blank")
        val pathParam = Uri.encode(assetJsPath ?: "")
        return "web_view/$urlParam/$pathParam"
    }
}


/**
 * 二级页面：显示特定学校和当前类别下的所有适配器列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdapterSelectionScreen(
    navController: NavController,
    schoolId: String,
    schoolName: String,
    categoryNumber: Int,
    resourceFolder: String,
    viewModel: SchoolSelectionViewModel = viewModel()
) {
    // 异步加载状态
    var adapters by remember { mutableStateOf<List<Adapter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 关键修正：从传入的 number 参数计算出当前的 AdapterCategory
    val currentCategory = AdapterCategory.forNumber(categoryNumber)
        ?: AdapterCategory.BACHELOR_AND_ASSOCIATE


    // 类别到中文名称的映射
    fun getCategoryDisplayName(): String {
        return when (currentCategory) { // 使用本地的 currentCategory 变量
            AdapterCategory.BACHELOR_AND_ASSOCIATE -> "本科/专科"
            AdapterCategory.POSTGRADUATE -> "研究生"
            AdapterCategory.GENERAL_TOOL -> "通用工具"
            else -> "其他"
        }
    }


    // 当 schoolId 或当前计算出的类别改变时，重新加载适配器列表
    LaunchedEffect(schoolId, currentCategory) {
        isLoading = true
        try {
            viewModel.updateSelectedCategory(currentCategory)

            val loadedAdapters = viewModel.getAdaptersForSchoolAndCategory(schoolId)
            adapters = loadedAdapters
        } catch (e: Exception) {
            println("Error loading adapters for $schoolName: ${e.message}")
            adapters = emptyList()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$schoolName - ${getCategoryDisplayName()}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回学校列表")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // 加载中状态
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                adapters.isEmpty() -> {
                    // 列表为空状态
                    Text(
                        text = "该学校在「${getCategoryDisplayName()}」类别下暂无适配器。",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // 显示适配器列表
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(adapters, key = { it.adapterId }) { adapter ->
                            AdapterCard(
                                adapter = adapter,
                                onClick = { selectedAdapter ->
                                    val initialUrl = selectedAdapter.importUrl.ifBlank { "about:blank" }

                                    val jsFileName = selectedAdapter.assetJsPath

                                    val assetJsPath = if (jsFileName.isNotBlank()) {
                                        "$resourceFolder/$jsFileName"
                                    } else {
                                        "$resourceFolder/${selectedAdapter.adapterId}.js"
                                    }
                                    // 导航到 WebView，传递 URL 和正确构建的 JS 路径
                                    navController.navigate(
                                        WebViewNavigationHelper.createRoute(
                                            initialUrl = initialUrl,
                                            assetJsPath = assetJsPath
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 适配器信息卡片 Composable。
 */
@Composable
fun AdapterCard(
    adapter: Adapter,
    onClick: (Adapter) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(adapter) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = adapter.adapterName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            // 描述
            Text(
                text = adapter.description.ifBlank { "无详细描述。" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "贡献者: ${adapter.maintainer.ifBlank { "未知" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}