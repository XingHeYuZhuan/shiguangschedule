package com.xingheyuzhuan.shiguangschedule.ui.settings.coursetables

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 负责管理课表管理界面的所有状态和业务逻辑。
 */
class ManageCourseTablesViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository
) : ViewModel() {

    // 组合两个数据流，提供一个包含课表列表和当前选中ID的单一UI状态
    val uiState: StateFlow<ManageCourseTablesUiState> = combine(
        courseTableRepository.getAllCourseTables(),
        appSettingsRepository.getAppSettings()
    ) { courseTables, appSettings ->
        ManageCourseTablesUiState(
            courseTables = courseTables,
            currentActiveTableId = appSettings.currentCourseTableId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManageCourseTablesUiState()
    )

    /**
     * 创建一个新的课表。
     * @param newTableName 新课表的名称。
     */
    fun createNewCourseTable(newTableName: String) {
        viewModelScope.launch {
            courseTableRepository.createNewCourseTable(newTableName)
        }
    }

    /**
     * 更新一个课表。
     * @param updatedCourseTable 包含新信息的课表对象。
     */
    fun updateCourseTable(updatedCourseTable: CourseTable) {
        viewModelScope.launch {
            courseTableRepository.updateCourseTable(updatedCourseTable)
        }
    }

    /**
     * 切换当前激活的课表。
     * @param tableId 要切换到的课表ID。
     */
    fun switchCourseTable(tableId: String) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettings().first()
            val newSettings = currentSettings.copy(currentCourseTableId = tableId)
            appSettingsRepository.insertOrUpdateAppSettings(newSettings)
        }
    }

    /**
     * 删除一个课表。
     * @param courseTable 要删除的课表对象。
     */
    fun deleteCourseTable(courseTable: CourseTable) {
        viewModelScope.launch {
            val success = courseTableRepository.deleteCourseTable(courseTable)
            if (success) {
                // 如果删除的是当前激活的课表，自动切换到列表中的第一个课表
                if (courseTable.id == uiState.value.currentActiveTableId) {
                    val remainingTables = courseTableRepository.getAllCourseTables().first()
                    if (remainingTables.isNotEmpty()) {
                        switchCourseTable(remainingTables.first().id)
                    }
                }
            }
        }
    }

    /**
     * 伴生对象，用于创建 ViewModel 实例。
     */
    companion object {
        fun provideFactory(
            application: Application
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ManageCourseTablesViewModel::class.java)) {
                    // 从 MyApplication 中获取仓库实例
                    val appSettingsRepository = (application as MyApplication).appSettingsRepository
                    val courseTableRepository = (application as MyApplication).courseTableRepository
                    return ManageCourseTablesViewModel(appSettingsRepository, courseTableRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

/**
 * 封装 UI 状态的数据类，减少 UI 层的复杂性。
 */
data class ManageCourseTablesUiState(
    val courseTables: List<CourseTable> = emptyList(),
    val currentActiveTableId: String? = null
)