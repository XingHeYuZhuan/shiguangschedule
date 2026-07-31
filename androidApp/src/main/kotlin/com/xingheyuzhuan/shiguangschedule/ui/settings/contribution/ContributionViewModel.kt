package com.xingheyuzhuan.shiguangschedule.ui.settings.contribution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.ContributionList
import com.xingheyuzhuan.shiguangschedule.data.repository.ContributionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * UI 状态的密封类
 */
sealed interface ContributionUiState {
    object Loading : ContributionUiState
    data class Success(val data: ContributionList) : ContributionUiState
    data class Error(val message: String) : ContributionUiState
}

@KoinViewModel
class ContributionViewModel(
    private val contributionRepository: ContributionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContributionUiState>(ContributionUiState.Loading)
    val uiState: StateFlow<ContributionUiState> = _uiState.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex = _selectedTabIndex.asStateFlow()

    init {
        loadContributions()
    }

    fun loadContributions() {
        viewModelScope.launch {
            _uiState.value = ContributionUiState.Loading
            try {
                val data = contributionRepository.getContributions()
                _uiState.value = ContributionUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = ContributionUiState.Error(e.message ?: "未知错误")
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }
}