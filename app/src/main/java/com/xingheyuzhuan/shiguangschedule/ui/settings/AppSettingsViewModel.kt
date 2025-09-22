package com.xingheyuzhuan.shiguangschedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettings
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 最终修正版 ViewModel。
 * 唯一的职责：在 UI 和 Repository 之间传递数据和事件。
 * 不包含任何计算或业务逻辑，所有逻辑都封装在 AppSettingsRepository 中。
 */
class SettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    // 直接从 Repository 获取 AppSettings 的数据流，并暴露给 UI
    val appSettingsState: StateFlow<AppSettings> = appSettingsRepository.getAppSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    private val _currentWeekState = MutableStateFlow<Int?>(null)
    val currentWeekState: StateFlow<Int?> = _currentWeekState

    init {
        updateCurrentWeek()
    }

    /**
     * 一个私有函数，用于更新当前周数的 StateFlow。
     */
    private fun updateCurrentWeek() {
        viewModelScope.launch {
            _currentWeekState.value = appSettingsRepository.calculateCurrentWeekFromDb()
        }
    }

    /**
     * UI 事件：更新是否显示周末。
     */
    fun onShowWeekendsChanged(show: Boolean) {
        viewModelScope.launch {
            val currentSettings = appSettingsState.value
            val newSettings = currentSettings.copy(showWeekends = show)
            appSettingsRepository.insertOrUpdateAppSettings(newSettings)
        }
    }

    /**
     * UI 事件：更新学期开始日期。
     */
    fun onSemesterStartDateSelected(selectedDateMillis: Long?) {
        viewModelScope.launch {
            if (selectedDateMillis != null) {
                val selectedDate = Instant.ofEpochMilli(selectedDateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val newSettings = appSettingsState.value.copy(
                    semesterStartDate = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
                appSettingsRepository.insertOrUpdateAppSettings(newSettings)
                updateCurrentWeek()
            }
        }
    }

    /**
     * UI 事件：更新学期总周数。
     */
    fun onSemesterTotalWeeksSelected(totalWeeks: Int) {
        viewModelScope.launch {
            val newSettings = appSettingsState.value.copy(semesterTotalWeeks = totalWeeks)
            appSettingsRepository.insertOrUpdateAppSettings(newSettings)
            updateCurrentWeek()
        }
    }

    /**
     * UI 事件：手动设置当前周数。
     * 接受一个可空的 Int? 类型，以支持“假期中”选项。
     */
    fun onCurrentWeekManuallySet(weekNumber: Int?) {
        viewModelScope.launch {
            appSettingsRepository.setSemesterStartDateFromWeek(weekNumber)
            updateCurrentWeek()
        }
    }
}
/**
 * ViewModel 的工厂类，用于依赖注入。
 * 在此文件中定义，以提高代码内聚性。
 */
object SettingsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val appSettingsRepository = (application as MyApplication).appSettingsRepository

            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(appSettingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
