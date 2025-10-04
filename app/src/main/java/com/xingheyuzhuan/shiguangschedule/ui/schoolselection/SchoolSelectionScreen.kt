package com.xingheyuzhuan.shiguangschedule.ui.schoolselection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.xingheyuzhuan.shiguangschedule.Screen
import com.xingheyuzhuan.shiguangschedule.data.SchoolRepository
import com.xingheyuzhuan.shiguangschedule.data.model.School
import com.xingheyuzhuan.shiguangschedule.data.model.SchoolCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SchoolSelectionScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var allSchools by remember { mutableStateOf<List<School>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 当前选中的类别，默认为本科/专科
    var selectedCategory by remember { mutableStateOf(SchoolCategory.BACHELOR_AND_ASSOCIATE) }

    LaunchedEffect(Unit) {
        allSchools = SchoolRepository.getSchools(context)
    }

    // 过滤逻辑现在依赖于 allSchools, searchQuery, 和 selectedCategory
    val filteredSchools = remember(allSchools, searchQuery, selectedCategory) {
        allSchools
            // 1. 优先按选中的类别进行过滤
            .filter { it.category == selectedCategory }
            // 2. 然后对该类别的学校应用搜索查询
            .let { categoryFiltered ->
                if (searchQuery.isBlank()) {
                    categoryFiltered
                } else {
                    categoryFiltered.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.initial.contains(searchQuery, ignoreCase = true)
                    }
                }
            }
            // 3. 最后按首字母排序
            .sortedBy { it.initial.uppercase() + it.name }
    }

    val initials = remember(filteredSchools) {
        filteredSchools.map { it.initial.uppercase() }.distinct().sorted()
    }

    Scaffold(
        topBar = {
            SearchBarWithTitle(
                navController = navController,
                searchQuery = searchQuery,
                onQueryChange = { newQuery -> searchQuery = newQuery },
                searchActive = searchActive,
                onSearchActiveChange = { active -> searchActive = active },
                placeholderText = "搜索学校名称或首字母",
                titleText = "选择学校",
                filteredSchools = filteredSchools
            ) { selectedSchool ->
                navController.navigate(Screen.WebView.createRoute(selectedSchool.id))
                searchActive = false
                searchQuery = ""
            }
        }
    ) { paddingValues ->
        if (!searchActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 类别选择器
                CategoryTabs(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        selectedCategory = category
                        // 切换类别时，将列表滚动到顶部，提升用户体验
                        coroutineScope.launch { lazyListState.scrollToItem(0) }
                    }
                )

                if (allSchools.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("正在加载学校数据...", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    if (filteredSchools.isEmpty()) {
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
                    } else {
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
                                        SchoolItem(school = school) { selectedSchool ->
                                            navController.navigate(Screen.WebView.createRoute(selectedSchool.id))
                                        }
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
        }
    }
}

/**
 * 类别选择器，将宽度均分为三个部分，使用扁平标签页样式。
 */
@Composable
fun CategoryTabs(
    selectedCategory: SchoolCategory,
    onCategorySelected: (SchoolCategory) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 遍历所有类别
            SchoolCategory.entries.forEach { category ->
                val isSelected = category == selectedCategory

                val backgroundColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                }

                val textColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(backgroundColor, shape = MaterialTheme.shapes.small)
                        .clickable { onCategorySelected(category) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.displayName,
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
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
    onSchoolSelected: (School) -> Unit
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
        if (filteredSchools.isEmpty()) {
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
            school.maintainer?.let { maintainer ->
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = maintainer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AlphabetIndex(
    initials: List<String>,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope,
    filteredSchools: List<School>
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

private fun scrollToInitial(
    targetInitial: String,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope,
    filteredSchools: List<School>
) {
    coroutineScope.launch {
        val itemIndex = filteredSchools.indexOfFirst {
            it.initial.uppercase() == targetInitial
        }

        if (itemIndex != -1) {
            lazyListState.animateScrollToItem(itemIndex)
        }
    }
}