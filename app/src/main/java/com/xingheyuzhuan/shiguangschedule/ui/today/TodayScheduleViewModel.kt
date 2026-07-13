package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayScheduleViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val styleSettingsRepository: StyleSettingsRepository,
    private val timeSlotRepository: TimeSlotRepository
) : ViewModel() {

    val gridStyle: StateFlow<ScheduleGridStyle> = styleSettingsRepository.styleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleGridStyle())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = appSettingsRepository.getAppSettings()
        .flatMapLatest { settings ->
            val tableId = settings.currentCourseTableId
            val today = LocalDate.now()
            val todayStr = today.toString()
            val dayOfWeek = today.dayOfWeek.value

            combine(
                appSettingsRepository.calculateCurrentWeekFromDb(),
                appSettingsRepository.getCourseTableConfigFlow(tableId),
                timeSlotRepository.getTimeSlotsByCourseTableId(tableId)
            ) { weekIndex: Int?, config: CourseTableConfig?, timeSlots: List<TimeSlot> ->

                val startDate = config?.semesterStartDate?.let {
                    try { LocalDate.parse(it) } catch (e: Exception) { null }
                }

                // 判定今天是否在跳过日期集合中
                val isSkippedDay = settings.skippedDates.contains(todayStr)

                val status = when {
                    config?.semesterStartDate == null -> TodayStatus.NoSemesterConfig
                    startDate != null && today.isBefore(startDate) -> TodayStatus.Vacation
                    weekIndex == null -> TodayStatus.SemesterEnded
                    else -> TodayStatus.Normal
                }

                // 仅在 ViewModel 内部快照中记录是否跳过
                DataSnapshot(status, weekIndex, timeSlots, startDate, isSkippedDay)
            }.flatMapLatest { snapshot ->
                // 只有 Normal 状态且不是跳过日期时才查询数据库
                if (snapshot.status == TodayStatus.Normal && snapshot.weekIndex != null && !snapshot.isSkippedDay) {
                    courseTableRepository.getCoursesForDay(tableId, snapshot.weekIndex, dayOfWeek).map { courses ->
                        createSuccessState(courses, snapshot, today)
                    }
                } else {
                    // 如果是跳过日期，直接返回空课程列表
                    // UI 层收到空列表会根据原有逻辑自动显示“今日无课”
                    flowOf(createSuccessState(emptyList(), snapshot, today))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState.Loading)

    /**
     * 内部辅助快照类
     */
    private data class DataSnapshot(
        val status: TodayStatus,
        val weekIndex: Int?,
        val timeSlots: List<TimeSlot>,
        val startDate: LocalDate?,
        val isSkippedDay: Boolean
    )

    private fun createSuccessState(
        courses: List<com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks>,
        snapshot: DataSnapshot,
        today: LocalDate
    ): TodayUiState.Success {
        val slotMap = snapshot.timeSlots.associateBy { it.number }

        val displayModels = courses.map { item ->
            val startSlot = slotMap[item.course.startSection]
            val endSlot = slotMap[item.course.endSection]
            CourseDisplayModel(
                course = item.course,
                startTime = item.course.customStartTime ?: startSlot?.startTime,
                endTime = item.course.customEndTime ?: endSlot?.endTime
            )
        }.sortedWith(
            compareBy<CourseDisplayModel> { it.startTime ?: "99:99" }
                .thenBy { it.endTime ?: "99:99" }
        )

        return TodayUiState.Success(
            courses = displayModels,
            weekIndex = snapshot.weekIndex ?: 0,
            today = today,
            status = snapshot.status,
            startDate = snapshot.startDate
        )
    }
}

data class CourseDisplayModel(val course: Course, val startTime: String?, val endTime: String?)

enum class TodayStatus { Normal, NoSemesterConfig, SemesterEnded, Vacation }

sealed class TodayUiState {
    data object Loading : TodayUiState()
    data class Success(
        val courses: List<CourseDisplayModel>,
        val weekIndex: Int,
        val today: LocalDate,
        val status: TodayStatus,
        val startDate: LocalDate?
    ) : TodayUiState()
}