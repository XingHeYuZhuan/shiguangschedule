package com.xingheyuzhuan.shiguangschedule.ui.settings.time

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * ViewModel，用于管理时间段设置界面的 UI 状态和业务逻辑。
 */
@KoinViewModel
class TimeSlotViewModel(
    private val timeSlotRepository: TimeSlotRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository
) : ViewModel() {

    // 获取应用设置的流，包括当前课表ID
    private val appSettingsFlow = appSettingsRepository.getAppSettings()

    // 拦截逻辑相关变量
    private var initialTimeSlots: List<TimeSlot> = emptyList()
    private var initialClassDuration: Int = 45
    private var initialBreakDuration: Int = 10
    private var isDataInitialized = false

    /**
     * 将时间段列表、默认上课时长和默认下课时长组合成一个单一的 UI 状态流。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val timeSlotsUiState: StateFlow<TimeSlotUiState> =
        appSettingsFlow
            .flatMapLatest { appSettings ->
                val currentTableId = appSettings.currentCourseTableId
                val timeSlotsFlow = timeSlotRepository.getTimeSlotsByCourseTableId(currentTableId)
                val courseConfigFlow = appSettingsRepository.getCourseTableConfigFlow(currentTableId)

                combine(timeSlotsFlow, courseConfigFlow) { timeSlots, config ->
                    val classDuration = config?.defaultClassDuration ?: 45
                    val breakDuration = config?.defaultBreakDuration ?: 10

                    if (!isDataInitialized) {
                        // 存储备份（去除 ID 差异带来的比对干扰，仅比对核心业务字段）
                        initialTimeSlots = timeSlots.map { it.copy(courseTableId = "") }.sortedBy { it.startTime }
                        initialClassDuration = classDuration
                        initialBreakDuration = breakDuration
                        isDataInitialized = true
                    }

                    TimeSlotUiState(
                        timeSlots = timeSlots,
                        defaultClassDuration = classDuration,
                        defaultBreakDuration = breakDuration,
                        isDataLoaded = true
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TimeSlotUiState(emptyList(), 45, 10, false)
            )

    /**
     * 供 UI 调用：比对当前内存中的数据与进入页面时的初始数据是否有差异
     */
    fun hasUnsavedChanges(
        currentTimeSlots: List<TimeSlot>,
        currentClassDuration: Int,
        currentBreakDuration: Int
    ): Boolean {
        // 如果数据还没加载好，不认为有修改
        if (!isDataInitialized) return false

        // 1. 检查时长设置是否有变
        if (currentClassDuration != initialClassDuration) return true
        if (currentBreakDuration != initialBreakDuration) return true

        // 2. 检查时间段列表是否有变
        if (currentTimeSlots.size != initialTimeSlots.size) return true

        val normalizedCurrent = currentTimeSlots.map { it.copy(courseTableId = "") }.sortedBy { it.startTime }

        return normalizedCurrent != initialTimeSlots
    }

    /**
     * 保存成功后更新备份点，这样点击返回就不会再触发拦截弹窗
     */
    private fun updateBackupPoint(timeSlots: List<TimeSlot>, classDuration: Int, breakDuration: Int) {
        initialTimeSlots = timeSlots.map { it.copy(courseTableId = "") }.sortedBy { it.startTime }
        initialClassDuration = classDuration
        initialBreakDuration = breakDuration
    }

    /**
     * UI 事件：一次性保存所有设置
     */
    fun onSaveAllSettings(
        timeSlots: List<TimeSlot>,
        classDuration: Int,
        breakDuration: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentTableId = appSettingsRepository.getAppSettings().first().currentCourseTableId
            val allTables = courseTableRepository.getAllCourseTables().first()
            val allTableIds = allTables.map { it.id }

            val tableExists = allTableIds.contains(currentTableId)

            if (tableExists) {
                // 确保时间段关联正确的课表 ID 后写入数据库
                val timeSlotsWithCorrectId = timeSlots.map { it.copy(courseTableId = currentTableId) }

                // 1. 替换时间段列表
                timeSlotRepository.replaceAllForCourseTable(currentTableId, timeSlotsWithCorrectId)

                // 2. 更新课表配置
                val currentConfig = appSettingsRepository.getCourseConfigOnce(currentTableId)
                    ?: CourseTableConfig(courseTableId = currentTableId)

                val updatedConfig = currentConfig.copy(
                    defaultClassDuration = classDuration,
                    defaultBreakDuration = breakDuration
                )

                // 3. 写入数据库
                appSettingsRepository.insertOrUpdateCourseConfig(updatedConfig)

                // 4. 重要：保存成功后同步备份状态
                updateBackupPoint(timeSlotsWithCorrectId, classDuration, breakDuration)

                onSuccess()
                Log.d("TimeSlotViewModel", "设置保存成功")
            } else {
                Log.e("TimeSlotViewModel", "无法保存设置：课表 ID 无效")
            }
        }
    }
}

data class TimeSlotUiState(
    val timeSlots: List<TimeSlot>,
    val defaultClassDuration: Int,
    val defaultBreakDuration: Int,
    val isDataLoaded: Boolean = false
)