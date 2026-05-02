package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * 课表展示块：封装单次或冲突课程
 * startSection/endSection：逻辑节次偏移量（0.0 代表第一节课顶部）
 */
data class MergedCourseBlock(
    val day: Int,
    val startSection: Float,
    val endSection: Float,
    val courses: List<CourseWithWeeks>,
    val isConflict: Boolean = false,
    val hasNonCurrentWeekCourses: Boolean = false,
    val needsProportionalRendering: Boolean = false,
    val isVisualDemoted: Boolean = false
)

data class WeeklyScheduleUiState(
    val style: ScheduleGridStyle = ScheduleGridStyle(),
    val showWeekends: Boolean = false,
    val totalWeeks: Int = 20,
    val timeSlots: List<TimeSlot> = emptyList(),
    val courseCache: Map<String, List<MergedCourseBlock>> = emptyMap(),
    val currentMergedCourses: List<MergedCourseBlock> = emptyList(),
    val isSemesterSet: Boolean = false,
    val semesterStartDate: LocalDate? = null,
    val firstDayOfWeek: Int = DayOfWeek.MONDAY.value,
    val weekIndexInPager: Int? = null,
    val currentWeekNumber: Int? = null,
    val pagerMondayDate: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val currentSectionIndex: Int = -1,
    val daysUntilStart: Long = 0
)

/**
 * 规范化课程坐标的中间对象
 */
private data class NormalizedCourse(
    val raw: CourseWithWeeks,
    val start: Float,
    val end: Float
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyScheduleViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val styleSettingsRepository: StyleSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyScheduleUiState())
    val uiState: StateFlow<WeeklyScheduleUiState> = _uiState.asStateFlow()

    private val _pagerMondayDate = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )

    private val appSettingsFlow = appSettingsRepository.getAppSettings()
    private val styleFlow = styleSettingsRepository.styleFlow

    private val courseTableConfigFlow = appSettingsFlow.flatMapLatest { settings ->
        val tableId = settings.currentCourseTableId
        if (tableId.isNotEmpty()) {
            appSettingsRepository.getCourseTableConfigFlow(tableId)
        } else {
            flowOf(null)
        }
    }

    private val timeSlotsFlow = appSettingsFlow.flatMapLatest { settings ->
        val tableId = settings.currentCourseTableId
        if (tableId.isNotEmpty()) {
            timeSlotRepository.getTimeSlotsByCourseTableId(tableId)
        } else {
            flowOf(emptyList())
        }
    }

    /**
     * 实现三周滑动窗口预加载
     * 监听当前页日期，同时拉取 [前一周, 本周, 后一周] 的数据并转为 Map 缓存
     */
    private val currentCoursesFlow = combine(
        _pagerMondayDate,
        appSettingsFlow,
        courseTableConfigFlow,
        timeSlotsFlow
    ) { date, settings, config, slots ->
        val tableId = settings.currentCourseTableId
        if (config != null) {
            val window = listOf(date.minusWeeks(1), date, date.plusWeeks(1))

            combine(window.map { day ->
                val pageWeekNum = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = day,
                    startDateStr = config.semesterStartDate,
                    firstDayOfWeekInt = config.firstDayOfWeek
                )

                val isWithinSemester = pageWeekNum != null && pageWeekNum in 1..config.semesterTotalWeeks

                // 获取数据流
                val coursesFlow = if (settings.showNonCurrentWeekCourses && isWithinSemester) {
                    // 读取全部课程，然后在 map 中进行过滤
                    courseTableRepository.getCoursesWithWeeksByTableId(tableId).map { allCourses ->
                        allCourses.filter { cw ->
                            // 仅保留包含当前页周数或更大周数的课程（即尚未结束的课程）
                            cw.weeks.any { it.weekNumber >= pageWeekNum }
                        }
                    }
                } else {
                    courseTableRepository.getCoursesWithWeeksByDate(tableId, day, config)
                }

                coursesFlow.map { courses ->
                    day.toString() to mergeCourses(courses, slots, pageWeekNum ?: -1)
                }
            }) { results -> results.toMap() }
        } else {
            flowOf(emptyMap())
        }
    }.flatMapLatest { it }

    init {
        viewModelScope.launch {
            val configAndTimeFlow = combine(
                appSettingsFlow,
                courseTableConfigFlow,
                styleFlow,
                _pagerMondayDate
            ) { settings, config, style, mondayDate ->
                ScheduleConfigPackage(settings, config, style, mondayDate)
            }

            combine(configAndTimeFlow, currentCoursesFlow, timeSlotsFlow) { configPkg, cache, timeSlots ->
                val config = configPkg.config
                val startDate = config?.semesterStartDate?.let { LocalDate.parse(it) }
                val firstDayOfWeekInt = config?.firstDayOfWeek ?: DayOfWeek.MONDAY.value
                val totalWeeks = config?.semesterTotalWeeks ?: 20
                val today = LocalDate.now()

                val currentWeekNum = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = today,
                    startDateStr = config?.semesterStartDate,
                    firstDayOfWeekInt = firstDayOfWeekInt
                )

                val weekIndex = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = configPkg.mondayDate,
                    startDateStr = config?.semesterStartDate,
                    firstDayOfWeekInt = firstDayOfWeekInt
                )

                val currentSectionIndex = calculateCurrentSectionIndex(timeSlots)

                // 计算距离开学天数
                val daysUntil = if (startDate != null && today.isBefore(startDate)) {
                    ChronoUnit.DAYS.between(today, startDate)
                } else 0L

                // 修正颜色（仅针对本周课程做检查以减小负担）
                val currentWeekCourses = cache[configPkg.mondayDate.toString()] ?: emptyList()
                fixInvalidCourseColors(currentWeekCourses.flatMap { it.courses }, configPkg.style)

                WeeklyScheduleUiState(
                    style = configPkg.style,
                    showWeekends = config?.showWeekends ?: false,
                    totalWeeks = totalWeeks,
                    courseCache = cache,
                    currentMergedCourses = cache[configPkg.mondayDate.toString()] ?: emptyList(),
                    timeSlots = timeSlots,
                    isSemesterSet = startDate != null,
                    semesterStartDate = startDate,
                    firstDayOfWeek = firstDayOfWeekInt,
                    weekIndexInPager = weekIndex,
                    currentWeekNumber = currentWeekNum,
                    pagerMondayDate = configPkg.mondayDate,
                    currentSectionIndex = currentSectionIndex,
                    daysUntilStart = daysUntil
                )
            }.collect { _uiState.value = it }
        }
    }

    private fun calculateCurrentSectionIndex(timeSlots: List<TimeSlot>): Int {
        if (timeSlots.isEmpty()) return -1
        val now = LocalTime.now()
        val currentMinutes = now.hour * 60 + now.minute

        timeSlots.forEachIndexed { index, slot ->
            val startParts = slot.startTime.split(":")
            val endParts = slot.endTime.split(":")

            if (startParts.size == 2 && endParts.size == 2) {
                val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
                val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

                if (currentMinutes in startMinutes until endMinutes) {
                    return index + 1
                }
            }
        }
        return -1
    }

    fun updatePagerDate(newDate: LocalDate) = _pagerMondayDate.update { newDate }

    fun switchCourseTable(tableId: String) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettingsOnce()
            val newSettings = currentSettings.copy(currentCourseTableId = tableId)
            appSettingsRepository.insertOrUpdateAppSettings(newSettings)
        }
    }

    private fun fixInvalidCourseColors(courses: List<CourseWithWeeks>, style: ScheduleGridStyle) {
        viewModelScope.launch {
            val validRange = style.courseColorMaps.indices
            courses.forEach { cw ->
                if (cw.course.colorInt !in validRange) {
                    courseTableRepository.updateCourseColor(cw.course.id, style.generateRandomColorIndex())
                }
            }
        }
    }

    /**
     * 计算逻辑节次位置。支持超出范围吸附及课间吸附。
     */
    private fun timeToLogicalScale(time: LocalTime, timeSlots: List<TimeSlot>): Float {
        if (timeSlots.isEmpty()) return 1.0f
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val sortedSlots = timeSlots.sortedBy { it.number }

        val firstSlotStart = LocalTime.parse(sortedSlots.first().startTime, formatter)
        val lastSlotEnd = LocalTime.parse(sortedSlots.last().endTime, formatter)

        if (!time.isAfter(firstSlotStart)) return 1.0f
        // 当时间超过或等于最后一节结束时间时，返回底部坐标
        if (!time.isBefore(lastSlotEnd)) return (sortedSlots.size + 1).toFloat()

        val currentSlot = sortedSlots.find {
            val s = LocalTime.parse(it.startTime, formatter)
            val e = LocalTime.parse(it.endTime, formatter)
            !time.isBefore(s) && !time.isAfter(e)
        }

        if (currentSlot != null) {
            val sTime = LocalTime.parse(currentSlot.startTime, formatter)
            val eTime = LocalTime.parse(currentSlot.endTime, formatter)
            val duration = ChronoUnit.MINUTES.between(sTime, eTime).coerceAtLeast(1)
            return currentSlot.number.toFloat() + (ChronoUnit.MINUTES.between(sTime, time).toFloat() / duration)
        }

        val nextSlot = sortedSlots.find { LocalTime.parse(it.startTime, formatter).isAfter(time) }
        return nextSlot?.number?.toFloat() ?: (sortedSlots.size + 1).toFloat()
    }

    /**
     * 合并并处理课程块
     */
    fun mergeCourses(courses: List<CourseWithWeeks>, timeSlots: List<TimeSlot>, currentWeek: Int): List<MergedCourseBlock> {
        if (timeSlots.isEmpty()) return emptyList()
        val maxSection = timeSlots.size.toFloat()
        val limit = maxSection + 1.0f // 课表绝对底部逻辑坐标
        val minSafeHeight = 0.3f

        val normalizedList = courses.mapNotNull { cw ->
            try {
                val c = cw.course
                var (s, e) = if (c.isCustomTime) {
                    val sTime = LocalTime.parse(c.customStartTime ?: return@mapNotNull null)
                    val eTime = LocalTime.parse(c.customEndTime ?: return@mapNotNull null)
                    timeToLogicalScale(sTime, timeSlots) to timeToLogicalScale(eTime, timeSlots)
                } else {
                    val start = c.startSection?.toFloat() ?: return@mapNotNull null
                    val end = c.endSection?.toFloat() ?: return@mapNotNull null
                    start to (end + 1f)
                }

                if (s >= limit) {
                    e = limit
                    s = limit - minSafeHeight
                } else if (e <= 1.0f) {
                    s = 1.0f
                    e = 1.0f + minSafeHeight
                }

                if (e - s < minSafeHeight) {
                    if (e + minSafeHeight <= limit) {
                        e = s + minSafeHeight
                    } else {
                        s = e - minSafeHeight
                    }
                }

                NormalizedCourse(cw, s.coerceIn(1.0f, limit - 0.1f), e.coerceIn(1.0f + 0.1f, limit))
            } catch (e: Exception) { null }
        }

        val result = mutableListOf<MergedCourseBlock>()
        normalizedList.groupBy { it.raw.course.day }.forEach { (day, dailyCourses) ->
            val sorted = dailyCourses.sortedBy { it.start }
            if (sorted.isEmpty()) return@forEach

            var currentGroup = mutableListOf(sorted[0])
            var currentMaxEnd = sorted[0].end

            for (i in 1 until sorted.size) {
                val item = sorted[i]
                if (item.start < currentMaxEnd) {
                    currentGroup.add(item)
                    currentMaxEnd = maxOf(currentMaxEnd, item.end)
                } else {
                    result.add(createMergedBlock(day, currentGroup, timeSlots.size, currentWeek))
                    currentGroup = mutableListOf(item)
                    currentMaxEnd = item.end
                }
            }
            result.add(createMergedBlock(day, currentGroup, timeSlots.size, currentWeek))
        }
        return result
    }

    private fun createMergedBlock(day: Int, group: List<NormalizedCourse>, totalSlots: Int, currentWeek: Int): MergedCourseBlock {
        val rawCourses: List<CourseWithWeeks> = group.map { it.raw }.distinct()

        val sortedCourses = rawCourses.sortedWith(object : Comparator<CourseWithWeeks> {
            override fun compare(o1: CourseWithWeeks, o2: CourseWithWeeks): Int {
                val contains1 = o1.weeks.any { it.weekNumber == currentWeek }
                val contains2 = o2.weeks.any { it.weekNumber == currentWeek }
                if (contains1 != contains2) return if (contains1) -1 else 1
                val s1 = o1.course.startSection ?: 0
                val s2 = o2.course.startSection ?: 0
                return s1.compareTo(s2)
            }
        })

        val currentWeekCoursesCount = sortedCourses.count { cw -> cw.weeks.any { it.weekNumber == currentWeek } }
        val totalCoursesCount = sortedCourses.size
        val hasNonCurrentWeekCoursesExist = sortedCourses.any { cw -> !cw.weeks.any { it.weekNumber == currentWeek } }

        val isConflict = currentWeekCoursesCount > 1
        val isVisualDemoted = currentWeekCoursesCount == 0
        val hasNonCurrentWeekCourses = hasNonCurrentWeekCoursesExist && totalCoursesCount > 1

        val minS = group.minOf { it.start }
        val maxE = group.maxOf { it.end }

        return MergedCourseBlock(
            day = day,
            startSection = (minS - 1f).coerceIn(0f, totalSlots.toFloat()),
            endSection = (maxE - 1f).coerceIn(0f, totalSlots.toFloat()),
            courses = sortedCourses,
            isConflict = isConflict,
            hasNonCurrentWeekCourses = hasNonCurrentWeekCourses,
            needsProportionalRendering = group.any { it.raw.course.isCustomTime },
            isVisualDemoted = isVisualDemoted
        )
    }
}

private data class ScheduleConfigPackage(
    val settings: AppSettingsModel,
    val config: CourseTableConfig?,
    val style: ScheduleGridStyle,
    val mondayDate: LocalDate
)