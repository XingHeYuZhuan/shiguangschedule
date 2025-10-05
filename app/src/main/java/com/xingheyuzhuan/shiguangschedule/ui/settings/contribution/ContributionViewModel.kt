package com.xingheyuzhuan.shiguangschedule.ui.settings.contribution

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.ContributionList
import com.xingheyuzhuan.shiguangschedule.data.repository.ContributionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * UI 状态的密封类，用于优雅地处理加载、成功和错误状态。
 */
sealed interface ContributionUiState {
    object Loading : ContributionUiState
    data class Success(val data: ContributionList) : ContributionUiState
    data class Error(val message: String) : ContributionUiState
}

class ContributionViewModel(application: Application) : AndroidViewModel(application) {

    // 暴露数据加载状态
    private val _uiState = MutableStateFlow<ContributionUiState>(ContributionUiState.Loading)
    val uiState: StateFlow<ContributionUiState> = _uiState

    // Tab 状态：0 = 教务适配 (JiaowuAdapter), 1 = 软件开发 (AppDev)
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex = _selectedTabIndex.asStateFlow()

    init {
        loadContributions()
    }

    /**
     * 异步加载贡献者数据。
     */
    fun loadContributions() {
        viewModelScope.launch {
            _uiState.value = ContributionUiState.Loading

            try {
                // 调用单例仓库，传入 Application Context
                val context = getApplication<Application>()
                val data = ContributionRepository.getContributions(context)

                _uiState.value = ContributionUiState.Success(data)
            } catch (e: IOException) {
                // 捕获 I/O 或解析错误
                _uiState.value = ContributionUiState.Error("数据加载失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 暴露给 UI 的 Tab 切换方法
     */
    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }
}