package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.R
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
    val needsProportionalRendering: Boolean = false
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
    val weekTitle: String = "",
    val currentWeekNumber: Int? = null,
    val pagerMondayDate: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val currentSectionIndex: Int = -1
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
            // 定义窗口日期列表
            val window = listOf(date.minusWeeks(1), date, date.plusWeeks(1))

            // 为窗口内的每一周开启数据监听并合并成 Map
            combine(window.map { day ->
                courseTableRepository.getCoursesWithWeeksByDate(tableId, day, config)
                    .map { courses -> day.toString() to mergeCourses(courses, slots) }
            }) { results -> results.toMap() }
        } else {
            flowOf(emptyMap())
        }
    }.flatMapLatest { it }

    private val _stringProviderFlow = MutableStateFlow<((Int, Array<out Any>) -> String)?>(null)

    fun setStringProvider(provider: (Int, Array<out Any>) -> String) {
        _stringProviderFlow.value = provider
    }

    init {
        viewModelScope.launch {
            val configAndTimeFlow = combine(
                appSettingsFlow,
                courseTableConfigFlow,
                styleFlow,
                _pagerMondayDate,
                _stringProviderFlow
            ) { settings, config, style, mondayDate, provider ->
                ScheduleConfigPackage(settings, config, style, mondayDate, provider)
            }

            combine(configAndTimeFlow, currentCoursesFlow, timeSlotsFlow) { configPkg, cache, timeSlots ->
                val config = configPkg.config
                val startDate = config?.semesterStartDate?.let { LocalDate.parse(it) }
                val firstDayOfWeekInt = config?.firstDayOfWeek ?: DayOfWeek.MONDAY.value
                val totalWeeks = config?.semesterTotalWeeks ?: 20

                val currentWeekNum = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = LocalDate.now(),
                    startDateStr = config?.semesterStartDate,
                    firstDayOfWeekInt = firstDayOfWeekInt
                )

                val weekIndex = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = configPkg.mondayDate,
                    startDateStr = config?.semesterStartDate,
                    firstDayOfWeekInt = firstDayOfWeekInt
                )

                val currentSectionIndex = calculateCurrentSectionIndex(timeSlots)

                // 修正颜色（仅针对本周课程做检查以减小负担）
                val currentWeekCourses = cache[configPkg.mondayDate.toString()] ?: emptyList()
                fixInvalidCourseColors(currentWeekCourses.flatMap { it.courses }, configPkg.style)

                WeeklyScheduleUiState(
                    style = configPkg.style,
                    showWeekends = config?.showWeekends ?: false,
                    totalWeeks = totalWeeks,
                    courseCache = cache, // 注入全量缓存
                    currentMergedCourses = cache[configPkg.mondayDate.toString()] ?: emptyList(),
                    timeSlots = timeSlots,
                    isSemesterSet = startDate != null,
                    semesterStartDate = startDate,
                    firstDayOfWeek = firstDayOfWeekInt,
                    weekIndexInPager = weekIndex,
                    weekTitle = generateTitle(weekIndex, startDate, totalWeeks, configPkg.provider),
                    currentWeekNumber = currentWeekNum,
                    pagerMondayDate = configPkg.mondayDate,
                    currentSectionIndex = currentSectionIndex
                )
            }.collect { _uiState.value = it }
        }
    }

    private fun generateTitle(
        weekIndex: Int?,
        startDate: LocalDate?,
        totalWeeks: Int,
        provider: ((Int, Array<out Any>) -> String)?
    ): String {
        val today = LocalDate.now()
        val p = provider ?: return "..."
        return when {
            startDate == null -> p(R.string.title_semester_not_set, emptyArray())
            today.isBefore(startDate) -> p(R.string.title_vacation_until_start, arrayOf(ChronoUnit.DAYS.between(today, startDate).toString()))
            weekIndex != null && weekIndex in 1..totalWeeks -> p(R.string.title_current_week, arrayOf(weekIndex.toString()))
            else -> p(R.string.title_vacation, emptyArray())
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

        val firstSlotEnd = LocalTime.parse(sortedSlots.first().endTime, formatter)
        val lastSlotStart = LocalTime.parse(sortedSlots.last().startTime, formatter)

        if (!time.isAfter(firstSlotEnd)) return 1.0f
        if (!time.isBefore(lastSlotStart)) return sortedSlots.last().number.toFloat()

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
        val prevSlot = sortedSlots.lastOrNull { LocalTime.parse(it.endTime, formatter).isBefore(time) }
        return nextSlot?.number?.toFloat() ?: (prevSlot?.number?.toFloat()?.plus(1.0f) ?: 1.0f)
    }

    /**
     * 合并并处理课程块
     */
    fun mergeCourses(courses: List<CourseWithWeeks>, timeSlots: List<TimeSlot>): List<MergedCourseBlock> {
        if (timeSlots.isEmpty()) return emptyList()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val normalized = courses.mapNotNull { cw ->
            try {
                val c = cw.course
                var (startScale, endScale) = if (c.isCustomTime) {
                    val sTime = LocalTime.parse(c.customStartTime ?: return@mapNotNull null, formatter)
                    val eTime = LocalTime.parse(c.customEndTime ?: return@mapNotNull null, formatter)
                    timeToLogicalScale(sTime, timeSlots) to timeToLogicalScale(eTime, timeSlots)
                } else {
                    val s = c.startSection?.toFloat() ?: return@mapNotNull null
                    val e = c.endSection?.toFloat() ?: return@mapNotNull null
                    s to (e + 1f)
                }

                if (endScale - startScale < 0.5f) endScale = startScale + 0.5f
                Triple(cw, startScale, endScale)
            } catch (e: Exception) { null }
        }

        val result = mutableListOf<MergedCourseBlock>()
        normalized.groupBy { it.first.course.day }.forEach { (day, daily) ->
            val sorted = daily.sortedBy { it.second }
            val usedIds = mutableSetOf<String>()

            sorted.forEach { base ->
                if (base.first.course.id in usedIds) return@forEach
                val overlaps = sorted.filter { it.second < base.third && it.third > base.second }
                if (overlaps.isEmpty()) return@forEach

                val minS = overlaps.minOf { it.second }
                val maxE = overlaps.maxOf { it.third }

                result.add(MergedCourseBlock(
                    day = day,
                    startSection = (minS - 1f).coerceIn(0f, timeSlots.size.toFloat() - 0.5f),
                    endSection = (maxE - 1f).coerceIn(0.5f, timeSlots.size.toFloat()),
                    courses = overlaps.map { it.first }.distinct(),
                    isConflict = overlaps.size > 1,
                    needsProportionalRendering = overlaps.any { it.first.course.isCustomTime }
                ))
                usedIds.addAll(overlaps.map { it.first.course.id })
            }
        }
        return result
    }
}

private data class ScheduleConfigPackage(
    val settings: AppSettingsModel,
    val config: CourseTableConfig?,
    val style: ScheduleGridStyle,
    val mondayDate: LocalDate,
    val provider: ((Int, Array<out Any>) -> String)?
)