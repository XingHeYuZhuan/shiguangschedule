package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport
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
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 课表中的合并课程块。
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
 */
data class WeeklyScheduleUiState(
    val showWeekends: Boolean = false,
    val totalWeeks: Int = 20,
    val timeSlots: List<TimeSlot> = emptyList(),
    val allCourses: List<CourseWithWeeks> = emptyList(),
    val isSemesterSet: Boolean = false,
    val semesterStartDate: LocalDate? = null,
    val firstDayOfWeek: Int = DayOfWeek.MONDAY.value,
    val currentWeekNumber: Int? = null
)

/**
 * 周课表页面的 ViewModel。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val timeSlotRepository: TimeSlotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyScheduleUiState())
    val uiState: StateFlow<WeeklyScheduleUiState> = _uiState.asStateFlow()

    // 1. 订阅全局 AppSettings (包含 currentCourseTableId)
    private val appSettingsFlow: Flow<AppSettings> = appSettingsRepository.getAppSettings()

    // 2. 订阅当前课表的配置 (依赖 AppSettings)
    private val courseTableConfigFlow: Flow<CourseTableConfig?> =
        appSettingsFlow.flatMapLatest { settings ->
            settings.currentCourseTableId?.let { tableId ->
                appSettingsRepository.getCourseTableConfigFlow(tableId)
            } ?: flowOf(null)
        }

    // 3. 订阅当前课表的时间段 (依赖 AppSettings)
    private val timeSlotsForCurrentTable: Flow<List<TimeSlot>> =
        appSettingsFlow.flatMapLatest { settings ->
            if (settings.currentCourseTableId != null) {
                timeSlotRepository.getTimeSlotsByCourseTableId(settings.currentCourseTableId)
            } else {
                flowOf(emptyList())
            }
        }

    // 4. 订阅当前课表的全部课程 (依赖 AppSettings)
    private val allCourses: Flow<List<CourseWithWeeks>> =
        appSettingsFlow.flatMapLatest { settings ->
            if (settings.currentCourseTableId != null) {
                courseTableRepository.getCoursesWithWeeksByTableId(settings.currentCourseTableId)
            } else {
                flowOf(emptyList())
            }
        }

    init {
        viewModelScope.launch {
            // 组合 appSettingsFlow, courseTableConfigFlow, timeSlots, allCourses
            combine(
                appSettingsFlow,
                courseTableConfigFlow, // 接收课表配置
                timeSlotsForCurrentTable,
                allCourses
            ) { _, config, timeSlots, allCoursesList ->

                val startDateString = config?.semesterStartDate

                val semesterStartDate: LocalDate? = try {
                    startDateString?.let { LocalDate.parse(it) }
                } catch (e: DateTimeParseException) {
                    null
                }

                val isSemesterSet = semesterStartDate != null
                // 使用 ?.let 安全地获取配置，提供默认值
                val totalWeeks = config?.semesterTotalWeeks ?: 20
                val firstDayOfWeek = config?.firstDayOfWeek ?: DayOfWeek.MONDAY.value
                val showWeekends = config?.showWeekends ?: false


                // 1. 计算当前周数
                val currentWeekNumber = if (semesterStartDate != null) {
                    calculateCurrentWeek(semesterStartDate, totalWeeks, firstDayOfWeek)
                } else {
                    null
                }

                fixInvalidCourseColors(allCoursesList)

                WeeklyScheduleUiState(
                    showWeekends = showWeekends,
                    totalWeeks = totalWeeks,
                    allCourses = allCoursesList,
                    timeSlots = timeSlots,
                    isSemesterSet = isSemesterSet,
                    semesterStartDate = semesterStartDate,
                    firstDayOfWeek = firstDayOfWeek,
                    currentWeekNumber = currentWeekNumber
                )
            }.collect { _uiState.value = it }
        }
    }

    /**
     * 遍历所有课程，检查颜色索引是否有效。如果无效（例如旧的 ARGB 值），
     * 则随机生成一个有效索引并更新数据库。
     */
    private fun fixInvalidCourseColors(courses: List<CourseWithWeeks>) = viewModelScope.launch {
        // 获取有效的颜色索引范围 (0 到 11)
        val validColorRange = CourseImportExport.COURSE_COLOR_MAPS.indices

        for (courseWithWeeks in courses) {
            val course = courseWithWeeks.course
            val colorInt = course.colorInt

            val isInvalid = colorInt !in validColorRange

            if (isInvalid) {
                // 1. 随机生成一个新的有效索引
                val newColorInt = CourseImportExport.getRandomColorIndex()

                // 2. 调用 Repository 更新数据库中的颜色索引
                courseTableRepository.updateCourseColor(
                    courseId = course.id,
                    newColorInt = newColorInt
                )
            }
        }
    }

    private fun getStartDayOfWeek(date: LocalDate, firstDayOfWeekInt: Int): LocalDate {
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)
        return date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    }

    private fun calculateCurrentWeek(
        semesterStartDate: LocalDate,
        totalWeeks: Int,
        firstDayOfWeekInt: Int
    ): Int? {
        val alignedStartDate = getStartDayOfWeek(semesterStartDate, firstDayOfWeekInt)
        val alignedToday = getStartDayOfWeek(LocalDate.now(), firstDayOfWeekInt)

        if (alignedToday.isBefore(alignedStartDate)) return null

        val diffWeeks = ChronoUnit.WEEKS.between(alignedStartDate, alignedToday).toInt()
        val calculatedWeek = diffWeeks + 1

        return if (calculatedWeek in 1..totalWeeks) calculatedWeek else null
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