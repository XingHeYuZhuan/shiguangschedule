package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.repository.SchoolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import school_index.AdapterCategory
import school_index.School

/**
 * 负责一级学校选择页面的数据管理、状态维护和过滤逻辑。
 */
class SchoolSelectionViewModel(application: Application) : AndroidViewModel(application) {

    // 完整的学校列表，从 Repository 加载
    private val _allSchools = MutableStateFlow<List<School>>(emptyList())

    // 搜索查询状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 当前选中的类别状态，默认为本科/专科
    private val _selectedCategory = MutableStateFlow(AdapterCategory.BACHELOR_AND_ASSOCIATE)
    val selectedCategory: StateFlow<AdapterCategory> = _selectedCategory

    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadSchools()
    }

    // 所有需要显示的类别（用于 UI Tab）
    val displayCategories: List<AdapterCategory> = listOf(
        AdapterCategory.BACHELOR_AND_ASSOCIATE,
        AdapterCategory.POSTGRADUATE,
        AdapterCategory.GENERAL_TOOL
    )

    /**
     * 结合所有过滤条件（类别和搜索）的过滤列表。
     * 当 _allSchools, _searchQuery, 或 _selectedCategory 变化时，重新计算。
     */
    val filteredSchools: StateFlow<List<School>> = combine(
        _allSchools,
        _searchQuery,
        _selectedCategory
    ) { allSchools, query, category ->
        // 1. 类别过滤：筛选出内部适配器列表中包含选中类别的学校
        val categoryFiltered = allSchools.filter { school ->
            school.adaptersList.any { adapter -> adapter.category == category }
        }

        // 2. 搜索过滤
        if (query.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { school ->
                school.name.contains(query, ignoreCase = true) ||
                        school.initial.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 获取当前过滤列表的首字母用于索引
    val initials: StateFlow<List<String>> = filteredSchools.map { schools ->
        schools.map { it.initial.uppercase() }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * 从 Repository 加载学校数据。
     */
    private fun loadSchools() {
        viewModelScope.launch {
            _isLoading.value = true
            val context = getApplication<Application>()
            val schools = SchoolRepository.getSchools(context)
            _allSchools.value = schools
            _isLoading.value = false
        }
    }

    /**
     * 更新搜索查询。
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 更新选中的类别。
     */
    fun updateSelectedCategory(category: AdapterCategory) {
        _selectedCategory.value = category
    }

    /**
     * 根据学校 ID 和当前选中的类别，获取适配器列表。
     * 此方法依赖于 SchoolRepository.getAdaptersForSchool 方法。
     */
    suspend fun getAdaptersForSchoolAndCategory(schoolId: String): List<school_index.Adapter> {
        val context = getApplication<Application>()
        // 1. 获取该学校的所有适配器
        val allAdapters = SchoolRepository.getAdaptersForSchool(context, schoolId)

        // 2. 使用 ViewModel 中当前的选中类别进行过滤
        val currentCategory = _selectedCategory.value

        return allAdapters.filter { adapter ->
            adapter.category == currentCategory
        }
    }
}