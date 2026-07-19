package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.SchoolHistoryModel
import com.xingheyuzhuan.shiguangschedule.ui.components.AlphabetIndexerList
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import school_index.AdapterCategory
import school_index.School

/**
 * 主学校选择屏幕，现在通过 ViewModel 管理状态和数据获取。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSelectionListScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: SchoolSelectionViewModel = koinViewModel()
) {
    // 观察 ViewModel 状态
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredSchools by viewModel.filteredSchools.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val schoolHistory by viewModel.schoolHistory.collectAsState()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isSearchActive by remember { mutableStateOf(false) }

    val titleText = stringResource(R.string.title_select_school)
    val placeholderText = stringResource(R.string.search_hint_school)

    Scaffold(
        topBar = {
            SearchBarWithTitle(
                onBack = onBack, // 使用传入的 onBack
                searchQuery = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                searchActive = isSearchActive,
                onSearchActiveChange = { active ->
                    isSearchActive = active
                    if (!active) {
                        viewModel.updateSearchQuery("")
                    }
                },
                placeholderText = placeholderText,
                titleText = titleText,
                filteredSchools = filteredSchools,
                onSchoolSelected = { selectedSchool ->
                    viewModel.saveLastSchool(selectedSchool)
                    onNavigate(
                        Destination.AdapterSelection(
                            schoolId = selectedSchool.id,
                            schoolName = selectedSchool.name,
                            categoryNumber = selectedCategory.value,
                            resourceFolder = selectedSchool.resource_folder
                        )
                    )
                    isSearchActive = false
                    viewModel.updateSearchQuery("")
                }
            )
        }
    ) { paddingValues ->
        if (!isSearchActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CategoryTabs(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        viewModel.updateSelectedCategory(category)
                        coroutineScope.launch { lazyListState.scrollToItem(0) }
                    },
                    displayCategories = viewModel.displayCategories
                )

                SchoolContent(
                    isLoading = isLoading,
                    filteredSchools = filteredSchools,
                    lazyListState = lazyListState,
                    selectedCategory = selectedCategory,
                    schoolHistory = schoolHistory,
                    onClearHistory = { viewModel.clearHistory(it) },
                    onSchoolSelected = { school, category ->
                        viewModel.saveLastSchool(school)
                        // 列表点击跳转
                        onNavigate(
                            Destination.AdapterSelection(
                                schoolId = school.id,
                                schoolName = school.name,
                                categoryNumber = category.value,
                                resourceFolder = school.resource_folder
                            )
                        )
                    }
                )
            }
        } else {
            // 搜索激活时，SearchBar 内部会处理全屏显示，这里留空防止 padding 重叠
            Box(modifier = Modifier.padding(paddingValues))
        }
    }
}

/**
 * 集中管理加载状态和列表显示。
 */
@Composable
private fun SchoolContent(
    isLoading: Boolean,
    filteredSchools: List<School>,
    lazyListState: LazyListState,
    selectedCategory: AdapterCategory,
    schoolHistory: SchoolHistoryModel,
    onClearHistory: (AdapterCategory) -> Unit,
    onSchoolSelected: (School, AdapterCategory) -> Unit
) {
    val recentRecord = when (selectedCategory) {
        AdapterCategory.BACHELOR_AND_ASSOCIATE -> schoolHistory.bachelor
        AdapterCategory.POSTGRADUATE -> schoolHistory.postgraduate
        AdapterCategory.GENERAL_TOOL -> schoolHistory.general
        else -> null
    }

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        filteredSchools.isEmpty() && !isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.text_no_adapter_for_category),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        else -> {
            AlphabetIndexerList(
                data = filteredSchools,
                getInitial = { it.initial },
                lazyListState = lazyListState,
                headerContent = {
                    if (recentRecord != null && !recentRecord.isEmpty) {
                        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                            Text(
                                text = stringResource(R.string.label_recent_visit),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                SchoolItem(
                                    school = recentRecord.toSchool(),
                                    onClick = { onSchoolSelected(it, selectedCategory) }
                                )
                                IconButton(
                                    onClick = { onClearHistory(selectedCategory) },
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.a11y_delete),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            ) { school ->
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                    SchoolItem(
                        school = school,
                        onClick = { onSchoolSelected(it, selectedCategory) }
                    )
                }
            }
        }
    }
}

/**
 * 类别选择器，使用最新的 PrimaryTabRow。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTabs(
    selectedCategory: AdapterCategory,
    onCategorySelected: (AdapterCategory) -> Unit,
    displayCategories: List<AdapterCategory>
) {
    @Composable
    fun getDisplayName(category: AdapterCategory): String {
        return when (category) {
            AdapterCategory.BACHELOR_AND_ASSOCIATE -> stringResource(R.string.category_bachelor_associate)
            AdapterCategory.POSTGRADUATE -> stringResource(R.string.category_postgraduate)
            AdapterCategory.GENERAL_TOOL -> stringResource(R.string.category_general_tool)
            else -> stringResource(R.string.category_other)
        }
    }

    val selectedIndex = displayCategories.indexOf(selectedCategory).coerceAtLeast(0)

    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier.fillMaxWidth(),
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex),
                width = 24.dp,
            )
        }
    ) {
        displayCategories.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = getDisplayName(category),
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                    )
                }
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
    onBack: () -> Unit,
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
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_back)
                        )
                    }
                },
                trailingIcon = {
                    if (!searchActive) {
                        IconButton(onClick = { onSearchActiveChange(true) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.a11y_search)
                            )
                        }
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.a11y_clear_search)
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.text_no_school_found), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredSchools) { school ->
                    SchoolItem(school = school) { onSchoolSelected(it) }
                }
            }
        }
    }
}

/**
 * 学校列表项
 */
@Composable
fun SchoolItem(school: School, onClick: (School) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick(school) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = stringResource(R.string.a11y_school_icon),
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = school.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}