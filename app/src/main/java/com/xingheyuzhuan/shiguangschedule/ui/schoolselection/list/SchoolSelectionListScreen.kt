package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.xingheyuzhuan.shiguangschedule.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import school_index.AdapterCategory
import school_index.School

/**
 * 主学校选择屏幕，现在通过 ViewModel 管理状态和数据获取。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SchoolSelectionListScreen(
    navController: NavController,
    // 注入 ViewModel
    viewModel: SchoolSelectionViewModel = viewModel()
) {
    // 观察 ViewModel 状态
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredSchools by viewModel.filteredSchools.collectAsState()
    val initials by viewModel.initials.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isSearchActive by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            SearchBarWithTitle(
                navController = navController,
                searchQuery = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                searchActive = isSearchActive,
                onSearchActiveChange = { active ->
                    isSearchActive = active
                    if (!active) {
                        viewModel.updateSearchQuery("")
                    }
                },
                placeholderText = "搜索学校名称或首字母",
                titleText = "选择学校",
                filteredSchools = filteredSchools,
            ) { selectedSchool ->
                navController.navigate(
                    Screen.AdapterSelection.createRoute(
                        selectedSchool.id,
                        selectedSchool.name,
                        selectedCategory.number,
                        selectedSchool.resourceFolder
                    )
                )
                isSearchActive = false
                viewModel.updateSearchQuery("")
            }
        }
    ) { paddingValues ->
        if (!isSearchActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 类别选择器
                CategoryTabs(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        viewModel.updateSelectedCategory(category)
                        coroutineScope.launch { lazyListState.scrollToItem(0) }
                    },
                    displayCategories = viewModel.displayCategories
                )

                // 加载/空状态/列表内容
                SchoolContent(
                    isLoading = isLoading,
                    filteredSchools = filteredSchools,
                    initials = initials,
                    lazyListState = lazyListState,
                    coroutineScope = coroutineScope,
                    selectedCategoryNumber = selectedCategory.number,
                    onSchoolSelected = { school, categoryNumber ->
                        navController.navigate(
                            Screen.AdapterSelection.createRoute(
                                school.id,
                                school.name,
                                categoryNumber,
                                school.resourceFolder
                            )
                        )
                    }
                )
            }
        } else {
            // 搜索激活状态下，让 SearchBar 处理其内部内容
            Spacer(modifier = Modifier.padding(paddingValues))
        }
    }
}

/**
 * 集中管理加载状态和列表显示。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SchoolContent(
    isLoading: Boolean,
    filteredSchools: List<School>,
    initials: List<String>,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope,
    selectedCategoryNumber: Int,
    onSchoolSelected: (School, Int) -> Unit
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        filteredSchools.isEmpty() && !isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "当前类别暂无适配，请选择其他类别或检查数据源。",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        else -> {
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var currentInitial = ""
                    filteredSchools.forEach { school ->
                        val initial = school.initial.uppercase()
                        if (initial != currentInitial) {
                            stickyHeader {
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            currentInitial = initial
                        }
                        item {
                            // 调用 onSchoolSelected 时传入 school 和 selectedCategoryNumber
                            SchoolItem(school = school, onClick = { onSchoolSelected(it, selectedCategoryNumber) })
                        }
                    }
                }
                AlphabetIndex(
                    initials = initials,
                    lazyListState = lazyListState,
                    coroutineScope = coroutineScope,
                    filteredSchools = filteredSchools
                )
            }
        }
    }
}

/**
 * 类别选择器，现在使用 Protobuf 的 AdapterCategory。
 */
@Composable
fun CategoryTabs(
    selectedCategory: AdapterCategory,
    onCategorySelected: (AdapterCategory) -> Unit,
    displayCategories: List<AdapterCategory>
) {
    // 类别到中文名称的映射
    fun getDisplayName(category: AdapterCategory): String {
        return when (category) {
            AdapterCategory.BACHELOR_AND_ASSOCIATE -> "本科/专科"
            AdapterCategory.POSTGRADUATE -> "研究生"
            AdapterCategory.GENERAL_TOOL -> "通用工具"
            else -> "其他"
        }
    }

    val selectedIndex = displayCategories.indexOf(selectedCategory)

    TabRow(
        selectedTabIndex = if (selectedIndex < 0) 0 else selectedIndex,
        modifier = Modifier.fillMaxWidth()
    ) {
        displayCategories.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onCategorySelected(category) },
                text = { Text(text = getDisplayName(category)) }
            )
        }
    }
}

/**
 * 带有标题和搜索功能的自定义 SearchBar 组件。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWithTitle(
    navController: NavController,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    placeholderText: String,
    titleText: String,
    filteredSchools: List<School>,
    onSchoolSelected: (School) -> Unit // 注意: 这个回调只在 SearchBar 内部使用，它通过 lambda 捕获了 selectedCategory.number
) {
    SearchBar(
        modifier = Modifier.fillMaxWidth(),
        inputField = {
            SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onSearch = { onSearchActiveChange(false) },
                expanded = searchActive,
                onExpandedChange = onSearchActiveChange,
                placeholder = { Text(if (searchActive) placeholderText else titleText) },
                leadingIcon = {
                    IconButton(onClick = {
                        if (searchActive) {
                            onSearchActiveChange(false)
                            onQueryChange("")
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                trailingIcon = {
                    if (!searchActive) {
                        IconButton(onClick = { onSearchActiveChange(true) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除搜索"
                            )
                        }
                    }
                }
            )
        },
        expanded = searchActive,
        onExpandedChange = onSearchActiveChange,
    ) {
        // 搜索结果内容
        if (filteredSchools.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("没有找到匹配的学校", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filteredSchools.forEach { school ->
                    item {
                        SchoolItem(school = school) { onSchoolSelected(it) }
                    }
                }
            }
        }
    }
}

/**
 * 学校列表项，已移除 maintainer 字段显示。
 */
@Composable
fun SchoolItem(school: School, onClick: (School) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(school) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "学校图标",
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = school.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 字母索引条。
 */
@Composable
fun AlphabetIndex(
    initials: List<String>,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope,
    filteredSchools: List<School> // Protobuf School
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(28.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        initials.forEachIndexed { index, initial ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable {
                        val targetInitial = initials[index]
                        scrollToInitial(
                            targetInitial,
                            lazyListState,
                            coroutineScope,
                            filteredSchools
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 滚动到指定首字母的第一个学校项目。
 */
private fun scrollToInitial(
    targetInitial: String,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope,
    filteredSchools: List<School>
) {
    coroutineScope.launch {
        // 查找目标首字母在列表中的第一个学校索引
        val itemIndex = filteredSchools.indexOfFirst {
            it.initial.uppercase() == targetInitial
        }

        if (itemIndex != -1) {
            lazyListState.animateScrollToItem(itemIndex)
        }
    }
}