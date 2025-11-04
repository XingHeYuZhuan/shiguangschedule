package com.xingheyuzhuan.shiguangschedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


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

    @OptIn(ExperimentalCoroutinesApi::class)
    val courseTableConfigState: StateFlow<CourseTableConfig?> = appSettingsState
        .flatMapLatest { appSettings ->
            val id = appSettings.currentCourseTableId
            if (id != null) {
                appSettingsRepository.getCourseTableConfigFlow(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
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
     * 逻辑：现在更新的是 CourseTableConfig
     */
    fun onShowWeekendsChanged(show: Boolean) {
        viewModelScope.launch {
            val currentId = appSettingsState.value.currentCourseTableId ?: return@launch
            // 从 DB 获取最新快照以进行更新
            val currentConfig = appSettingsRepository.getCourseConfigOnce(currentId) ?: return@launch

            val newConfig = currentConfig.copy(showWeekends = show)
            appSettingsRepository.insertOrUpdateCourseConfig(newConfig)
        }
    }

    /**
     * UI 事件：更新学期开始日期。
     * 逻辑：现在更新的是 CourseTableConfig
     */
    fun onSemesterStartDateSelected(selectedDateMillis: Long?) {
        viewModelScope.launch {
            if (selectedDateMillis != null) {
                val currentId = appSettingsState.value.currentCourseTableId ?: return@launch
                // 从 DB 获取最新快照以进行更新
                val currentConfig = appSettingsRepository.getCourseConfigOnce(currentId) ?: return@launch

                val selectedDate = Instant.ofEpochMilli(selectedDateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val newConfig = currentConfig.copy(
                    semesterStartDate = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
                appSettingsRepository.insertOrUpdateCourseConfig(newConfig)
                updateCurrentWeek()
            }
        }
    }

    /**
     * UI 事件：更新学期总周数。
     * 逻辑：现在更新的是 CourseTableConfig
     */
    fun onSemesterTotalWeeksSelected(totalWeeks: Int) {
        viewModelScope.launch {
            val currentId = appSettingsState.value.currentCourseTableId ?: return@launch
            val currentConfig = appSettingsRepository.getCourseConfigOnce(currentId) ?: return@launch

            val newConfig = currentConfig.copy(semesterTotalWeeks = totalWeeks)
            appSettingsRepository.insertOrUpdateCourseConfig(newConfig)
            updateCurrentWeek()
        }
    }

    /**
     * UI 事件：手动设置当前周数。
     * 接受一个可空的 Int? 类型，以支持“假期中”选项。
     * 逻辑：Repository 内部会处理 AppSettings 和 CourseTableConfig 的获取和更新，所以调用不变。
     */
    fun onCurrentWeekManuallySet(weekNumber: Int?) {
        viewModelScope.launch {
            appSettingsRepository.setSemesterStartDateFromWeek(weekNumber)
            updateCurrentWeek()
        }
    }

    /**
     * UI 事件：更新每周起始日。
     * 逻辑：现在更新的是 CourseTableConfig 中的 firstDayOfWeek 字段。
     */
    fun onFirstDayOfWeekSelected(dayOfWeekInt: Int) {
        viewModelScope.launch {
            val currentId = appSettingsState.value.currentCourseTableId ?: return@launch
            // 从 DB 获取最新快照以进行更新
            val currentConfig = appSettingsRepository.getCourseConfigOnce(currentId) ?: return@launch

            val newConfig = currentConfig.copy(firstDayOfWeek = dayOfWeekInt)
            appSettingsRepository.insertOrUpdateCourseConfig(newConfig)
            // 修改了周起始日，需要重新计算当前周数
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