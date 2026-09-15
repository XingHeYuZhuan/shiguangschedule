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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.model.SchoolHistoryModel
import com.xingheyuzhuan.shiguangschedule.ui.components.AlphabetIndexerList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import school_index.AdapterCategory
import school_index.School
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_clear_search
import shiguangschedule.shared.generated.resources.a11y_delete
import shiguangschedule.shared.generated.resources.a11y_school_icon
import shiguangschedule.shared.generated.resources.a11y_search
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.category_bachelor_associate
import shiguangschedule.shared.generated.resources.category_general_tool
import shiguangschedule.shared.generated.resources.category_other
import shiguangschedule.shared.generated.resources.category_postgraduate
import shiguangschedule.shared.generated.resources.close_24px
import shiguangschedule.shared.generated.resources.label_recent_visit
import shiguangschedule.shared.generated.resources.school_24px
import shiguangschedule.shared.generated.resources.search_24px
import shiguangschedule.shared.generated.resources.search_hint_school
import shiguangschedule.shared.generated.resources.text_no_adapter_for_category
import shiguangschedule.shared.generated.resources.text_no_school_found
import shiguangschedule.shared.generated.resources.title_select_school
import shiguangschedule.shared.generated.resources.title_update_repo_screen
import shiguangschedule.shared.generated.resources.update_24px

/**
 * 学校选择主界面，包含搜索栏、分类 Tab 页签与学校索引列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSelectionListScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: SchoolSelectionViewModel = koinViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredSchools by viewModel.filteredSchools.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val schoolHistory by viewModel.schoolHistory.collectAsState()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var expanded by rememberSaveable { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState(initialText = searchQuery)

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text }.collect { text ->
            viewModel.updateSearchQuery(text.toString())
        }
    }

    val titleText = stringResource(Res.string.title_select_school)
    val placeholderText = stringResource(Res.string.search_hint_school)

    Scaffold(
        modifier = Modifier.semantics { isTraversalGroup = true },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .semantics { traversalIndex = 0f },
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = textFieldState.text.toString(),
                            onQueryChange = { newText ->
                                textFieldState.clearText()
                                textFieldState.edit { append(newText) }
                            },
                            onSearch = { expanded = false },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            placeholder = {
                                Text(if (expanded) placeholderText else titleText)
                            },
                            leadingIcon = {
                                IconButton(onClick = {
                                    if (expanded) {
                                        expanded = false
                                        textFieldState.clearText()
                                    } else {
                                        onBack()
                                    }
                                }) {
                                    Icon(
                                        imageVector = vectorResource(Res.drawable.arrow_back_24px),
                                        contentDescription = stringResource(Res.string.a11y_back)
                                    )
                                }
                            },
                            trailingIcon = {
                                if (!expanded) {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            imageVector = vectorResource(Res.drawable.search_24px),
                                            contentDescription = stringResource(Res.string.a11y_search)
                                        )
                                    }
                                } else if (textFieldState.text.isNotEmpty()) {
                                    IconButton(onClick = { textFieldState.clearText() }) {
                                        Icon(
                                            imageVector = vectorResource(Res.drawable.close_24px),
                                            contentDescription = stringResource(Res.string.a11y_clear_search)
                                        )
                                    }
                                }
                            }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    if (filteredSchools.isEmpty() && textFieldState.text.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(Res.string.text_no_school_found),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredSchools) { school ->
                                SchoolItem(school = school) { selectedSchool ->
                                    viewModel.saveLastSchool(selectedSchool)
                                    onNavigate(
                                        Destination.AdapterSelection(
                                            schoolId = selectedSchool.id,
                                            schoolName = selectedSchool.name,
                                            categoryNumber = selectedCategory.value,
                                            resourceFolder = selectedSchool.resource_folder
                                        )
                                    )
                                    expanded = false
                                    textFieldState.clearText()
                                }
                            }
                        }
                    }
                }

                if (!expanded) {
                    IconButton(
                        onClick = { onNavigate(Destination.UpdateRepo) },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.update_24px),
                            contentDescription = stringResource(Res.string.title_update_repo_screen)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (!expanded) {
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
        }
    }
}

/**
 * 展示学校列表面板（含加载指示器、空状态提示与最近访问历史）。
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
        filteredSchools.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.text_no_adapter_for_category),
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
                                text = stringResource(Res.string.label_recent_visit),
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
                                        imageVector = vectorResource(Res.drawable.close_24px),
                                        contentDescription = stringResource(Res.string.a11y_delete),
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
                Box(modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
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
 * 类型分类切换 Tab 页签。
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
            AdapterCategory.BACHELOR_AND_ASSOCIATE -> stringResource(Res.string.category_bachelor_associate)
            AdapterCategory.POSTGRADUATE -> stringResource(Res.string.category_postgraduate)
            AdapterCategory.GENERAL_TOOL -> stringResource(Res.string.category_general_tool)
            else -> stringResource(Res.string.category_other)
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
 * 单个学校卡片项。
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
                imageVector = vectorResource(Res.drawable.school_24px),
                contentDescription = stringResource(Res.string.a11y_school_icon),
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