package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 课表中的合并课程块。
 *
 * @param day 星期几 (1=周一, 7=周日)。
 * @param startSection 开始节次。
 * @param endSection 结束节次。
 * @param courses 包含在此块中的课程列表。
 * @param isConflict 是否存在课程冲突。
 */
data class MergedCourseBlock(
    val day: Int,
    val startSection: Int,
    val endSection: Int,
    val courses: List<CourseWithWeeks>,
    val isConflict: Boolean = false
)

/**
 * 周课表 UI 的所有状态。
 *
 * @param showWeekends 是否显示周末。
 * @param totalWeeks 学期总周数。
 * @param timeSlots 时间段列表。
 * @param allCourses 包含所有周的课程。
 * @param isSemesterSet 是否已设置开学日期。
 * @param semesterStartDate 学期开始日期，用于计算假期天数。
 */
data class WeeklyScheduleUiState(
    val showWeekends: Boolean = false,
    val totalWeeks: Int = 20,
    val timeSlots: List<TimeSlot> = emptyList(),
    val allCourses: List<CourseWithWeeks> = emptyList(), // 包含所有周的课程
    val isSemesterSet: Boolean = false,
    val semesterStartDate: LocalDate? = null,
)

/**
 * 周课表页面的 ViewModel。
 * 它持有 UI 状态，并与数据仓库交互。
 * 职责：只负责提供原始数据，不处理 UI 逻辑。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val timeSlotRepository: TimeSlotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyScheduleUiState())
    val uiState: StateFlow<WeeklyScheduleUiState> = _uiState.asStateFlow()

    private val allCourses: Flow<List<CourseWithWeeks>> =
        appSettingsRepository.getAppSettings()
            .flatMapLatest { settings ->
                if (settings.currentCourseTableId != null) {
                    courseTableRepository.getCoursesWithWeeksByTableId(settings.currentCourseTableId)
                } else {
                    flowOf(emptyList())
                }
            }

    private val timeSlotsForCurrentTable: Flow<List<TimeSlot>> =
        appSettingsRepository.getAppSettings()
            .flatMapLatest { settings ->
                if (settings.currentCourseTableId != null) {
                    timeSlotRepository.getTimeSlotsByCourseTableId(settings.currentCourseTableId)
                } else {
                    flowOf(emptyList())
                }
            }

    init {
        viewModelScope.launch {
            combine(
                appSettingsRepository.getAppSettings(),
                timeSlotsForCurrentTable,
                allCourses
            ) { settings, timeSlots, allCoursesList ->
                val semesterStartDate = try {
                    settings.semesterStartDate?.let { LocalDate.parse(it) }
                } catch (e: DateTimeParseException) {
                    null
                }

                val isSemesterSet = semesterStartDate != null

                val totalWeeks: Int = if (isSemesterSet) {
                    settings.semesterTotalWeeks
                } else {
                    20 // 默认值
                }

                WeeklyScheduleUiState(
                    showWeekends = settings.showWeekends,
                    totalWeeks = totalWeeks,
                    allCourses = allCoursesList,
                    timeSlots = timeSlots,
                    isSemesterSet = isSemesterSet,
                    semesterStartDate = semesterStartDate,
                )
            }.collect { _uiState.value = it }
        }
    }
}

/**
 * 合并课程块，处理连续课程和冲突课程。
 */
fun mergeCourses(
    courses: List<CourseWithWeeks>,
    timeSlots: List<TimeSlot>
): List<MergedCourseBlock> {
    val mergedBlocks = mutableListOf<MergedCourseBlock>()
    val coursesByDay = courses.groupBy { it.course.day }

    for ((day, dailyCourses) in coursesByDay) {
        val coursesSorted = dailyCourses.sortedBy { it.course.startSection }
        val processedCourses = mutableSetOf<CourseWithWeeks>()

        for (course in coursesSorted) {
            if (!processedCourses.contains(course)) {
                val combinedCourses = mutableListOf(course)
                var currentStartSection = course.course.startSection
                var currentEndSection = course.course.endSection
                var isConflict = false

                val overlappingCourses = coursesSorted.filter { other ->
                    other != course &&
                            !(other.course.endSection < currentStartSection ||
                                    other.course.startSection > currentEndSection)
                }

                if (overlappingCourses.isNotEmpty()) {
                    isConflict = true
                    combinedCourses.addAll(overlappingCourses)
                    currentStartSection = combinedCourses.minOf { it.course.startSection }
                    currentEndSection = combinedCourses.maxOf { it.course.endSection }
                }

                mergedBlocks.add(
                    MergedCourseBlock(
                        day = day,
                        startSection = currentStartSection,
                        endSection = currentEndSection,
                        courses = combinedCourses.distinct(),
                        isConflict = isConflict
                    )
                )
                processedCourses.addAll(combinedCourses)
            }
        }
    }
    return mergedBlocks
}


object WeeklyScheduleViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

        if (modelClass.isAssignableFrom(WeeklyScheduleViewModel::class.java)) {
            val myApplication = application as MyApplication
            @Suppress("UNCHECKED_CAST")
            return WeeklyScheduleViewModel(
                appSettingsRepository = myApplication.appSettingsRepository,
                courseTableRepository = myApplication.courseTableRepository,
                timeSlotRepository = myApplication.timeSlotRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}