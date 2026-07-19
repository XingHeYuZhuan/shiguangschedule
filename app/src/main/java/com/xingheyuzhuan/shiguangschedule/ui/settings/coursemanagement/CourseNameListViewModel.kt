package com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

/**
 * 用于 UI 展示的课程名称和实例数量的组合。
 */
data class CourseNameCount(
    val name: String,
    val count: Int
)

/**
 * 【一级页面】课程名称列表 ViewModel (Master View)
 * 负责提取当前课表下的唯一课程名称列表及其对应的实例数量。
 */
@KoinViewModel
class CourseNameListViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository
) : ViewModel() {

    // 1. 获取当前激活的课表ID的 Flow
    private val currentTableIdFlow = appSettingsRepository.getAppSettings()
        .map { it.currentCourseTableId }
        .distinctUntilChanged()

    /**
     * 暴露给 UI 的课程名称和实例数量列表 (List<CourseNameCount>)。
     * 订阅当前课表ID的变化，并获取该课表下的所有课程数据，然后进行分组和计数。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uniqueCourseNames: StateFlow<List<CourseNameCount>> = currentTableIdFlow
        .flatMapLatest { tableId ->
            if (tableId.isEmpty()) {
                flowOf(emptyList())
            } else {
                // 调用 Repository 获取当前课表下的所有课程及其周次信息 (List<CourseWithWeeks>)
                courseTableRepository.getCoursesWithWeeksByTableId(tableId)
            }
        }
        .map { coursesWithWeeks ->
            coursesWithWeeks
                .groupBy { it.course.name }
                .map { (name, list) ->
                    CourseNameCount(
                        name = name,
                        count = list.size
                    )
                }
                .sortedBy { it.name }
        }
        .stateIn(
            scope = viewModelScope,
            // 在 UI 活跃时保持数据流活跃
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     *获取当前课表ID，并请求 Repository 删除指定名称的课程实例。
     * @param courseNames 要删除的课程名称列表。
     */
    suspend fun deleteSelectedCourses(courseNames: List<String>) {
        if (courseNames.isEmpty()) return
        val appSettings = appSettingsRepository.getAppSettings().first()
        val tableId = appSettings.currentCourseTableId

        courseTableRepository.deleteCoursesByNames(tableId, courseNames)
    }
}