package com.xingheyuzhuan.shiguangschedule.ui.settings.time

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * ViewModel，用于管理时间段设置界面的 UI 状态和业务逻辑。
 * 它通过 Repository 与数据库进行交互，并为 UI 提供数据流。
 */
class TimeSlotViewModel(
    private val timeSlotRepository: TimeSlotRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository
) : ViewModel() {

    // 获取应用设置的流，包括当前课表ID
    private val appSettingsFlow = appSettingsRepository.getAppSettings()


    /**
     * 将时间段列表、默认上课时长和默认下课时长组合成一个单一的 UI 状态流。
     * 这个 StateFlow 会自动收集数据并将其暴露给 UI。
     *
     * 【重要改动】这里使用 flatMapLatest 来监听 currentCourseTableId 的变化，
     * 当它变化时，会自动切换到新的时间段列表流。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val timeSlotsUiState: StateFlow<TimeSlotUiState> =
        appSettingsFlow
            .flatMapLatest { appSettings ->
                // 如果当前课表ID存在，则获取该课表的时间段流
                val currentTableId = appSettings.currentCourseTableId
                if (currentTableId != null) {
                    // combine 将时间段列表流和 appSettings 流结合起来
                    combine(
                        timeSlotRepository.getTimeSlotsByCourseTableId(currentTableId),
                        flowOf(appSettings) // 将 appSettings 包装成一个流
                    ) { timeSlots, settings ->
                        TimeSlotUiState(
                            timeSlots = timeSlots,
                            defaultClassDuration = settings.defaultClassDuration,
                            defaultBreakDuration = settings.defaultBreakDuration
                        )
                    }
                } else {
                    // 如果当前课表ID为空，则返回一个包含空列表的默认状态流
                    flowOf(
                        TimeSlotUiState(
                            timeSlots = emptyList(),
                            defaultClassDuration = appSettings.defaultClassDuration,
                            defaultBreakDuration = appSettings.defaultBreakDuration
                        )
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TimeSlotUiState(emptyList(), 45, 10)
            )

    /**
     * UI 事件：一次性保存所有设置，包括时间段列表和默认时长。
     * 这是最安全和推荐的保存方式，避免了数据不一致的问题。
     */
    fun onSaveAllSettings(
        timeSlots: List<TimeSlot>,
        classDuration: Int,
        breakDuration: Int
    ) {
        viewModelScope.launch {
            val currentTableId = appSettingsRepository.getAppSettings().first().currentCourseTableId
            val allTables = courseTableRepository.getAllCourseTables().first()
            val allTableIds = allTables.map { it.id }

            val tableExists = allTableIds.contains(currentTableId)

            if (currentTableId != null && tableExists) {
                val timeSlotsWithCorrectId = timeSlots.map { it.copy(courseTableId = currentTableId) }

                timeSlotRepository.replaceAllForCourseTable(currentTableId, timeSlotsWithCorrectId)
                val newSettings = appSettingsRepository.getAppSettings().first().copy(
                    defaultClassDuration = classDuration,
                    defaultBreakDuration = breakDuration
                )

                // 4. 将新数据写入数据库
                appSettingsRepository.insertOrUpdateAppSettings(newSettings)
                Log.d("TimeSlotViewModel", "Settings saved successfully for table: $currentTableId")
            } else {
                Log.e("TimeSlotViewModel", "Cannot save settings. The current CourseTable ID is invalid or not found.")
            }
        }
    }
}

data class TimeSlotUiState(
    val timeSlots: List<TimeSlot>,
    val defaultClassDuration: Int,
    val defaultBreakDuration: Int
)

/**
 * ViewModel 的工厂类，用于依赖注入。
 */
object TimeSlotViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        val myApplication = application as MyApplication

        val appSettingsRepository = myApplication.appSettingsRepository
        val timeSlotRepository = myApplication.timeSlotRepository
        val courseTableRepository = myApplication.courseTableRepository

        if (modelClass.isAssignableFrom(TimeSlotViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeSlotViewModel(timeSlotRepository, appSettingsRepository, courseTableRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
