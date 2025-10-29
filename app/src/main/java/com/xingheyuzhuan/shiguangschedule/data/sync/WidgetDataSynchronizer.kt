package com.xingheyuzhuan.shiguangschedule.data.sync

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettings
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 负责主数据库和 Widget 数据库之间的数据同步。
 * 它持续监听数据变化，并自动将数据处理后存入为 Widget 优化的数据库。
 */
class WidgetDataSynchronizer(
    private val appContext: Context,
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val widgetRepository: WidgetRepository
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    private val WIDGET_SYNC_DAYS = 7L // 同步未来7天的数据

    /**
     * 根据日期和设置的一周起始日，推算出该日期所在周的起始日。
     * 用于周数对齐计算，逻辑与 AppSettingsRepository 中保持一致。
     */
    private fun getStartDayOfWeek(date: LocalDate, firstDayOfWeekInt: Int): LocalDate {
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)
        return date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    }

    /**
     * 一个持续发出 Unit 的 Flow，外部只需收集这个 Flow 即可触发同步。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val syncFlow: Flow<Unit> = appSettingsRepository.getAppSettings()
        .flatMapLatest { appSettings ->
            val tableId = appSettings?.currentCourseTableId

            if (tableId != null) {
                // 1. 课程列表 Flow
                val coursesFlow = courseTableRepository.getCoursesWithWeeksByTableId(tableId)
                // 2. 时间段列表 Flow
                val timeSlotsFlow = timeSlotRepository.getTimeSlotsByCourseTableId(tableId)

                val configFlow = appSettingsRepository.getCourseTableConfigFlow(tableId)

                combine(
                    coursesFlow,
                    timeSlotsFlow,
                    configFlow
                ) { courses, timeSlots, config ->
                    Quadruple(appSettings, courses, timeSlots, config)
                }
            } else {
                flowOf(Quadruple(appSettings, emptyList(), emptyList(), null))
            }
        }.combine(flowOf(Unit)) { (appSettings, coursesWithWeeks, timeSlots, config), _ ->
            if (appSettings != null && config != null) {
                performSync(appSettings, config, coursesWithWeeks, timeSlots)
            } else {
                widgetRepository.deleteAll()
                widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
                updateAllWidgets(appContext)
            }
        }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


    /**
     * 这是一个公共的挂起函数，用于手动触发一次性数据同步。
     */
    suspend fun syncNow() {
        val appSettings = appSettingsRepository.getAppSettings().first()
        val tableId = appSettings?.currentCourseTableId

        // 1. 获取 Courses 和 TimeSlots
        val coursesWithWeeks = if (tableId != null) courseTableRepository.getCoursesWithWeeksByTableId(tableId).first() else emptyList()
        val timeSlots = if (tableId != null) timeSlotRepository.getTimeSlotsByCourseTableId(tableId).first() else emptyList()

        // 2. 获取 CourseTableConfig
        val courseConfig = if (tableId != null) appSettingsRepository.getCourseConfigOnce(tableId) else null

        if (appSettings != null && courseConfig != null) {
            performSync(appSettings, courseConfig, coursesWithWeeks, timeSlots)
        } else {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            updateAllWidgets(appContext)
        }
    }

    /**
     * 实际执行同步逻辑的私有方法，避免代码重复。
     */
    private suspend fun performSync(
        appSettings: AppSettings,
        courseConfig: CourseTableConfig,
        coursesWithWeeks: List<CourseWithWeeks>,
        timeSlots: List<TimeSlot>
    ) {
        // 核心逻辑：从 CourseConfig 获取数据
        val semesterStartDateString = courseConfig.semesterStartDate ?: run {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            updateAllWidgets(appContext)
            return
        }
        val semesterTotalWeeks = courseConfig.semesterTotalWeeks
        val firstDayOfWeekInt = courseConfig.firstDayOfWeek

        if (semesterTotalWeeks <= 0) {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            updateAllWidgets(appContext)
            return
        }

        val widgetSettings = WidgetAppSettings(
            id = 1,
            semesterStartDate = semesterStartDateString,
            semesterTotalWeeks = semesterTotalWeeks,
            firstDayOfWeek = firstDayOfWeekInt
        )
        widgetRepository.insertOrUpdateAppSettings(widgetSettings)

        // 保持不变：从 AppSettings 获取数据
        val skippedDates = appSettings.skippedDates ?: emptySet()
        val timeSlotMap = timeSlots.associateBy { it.number }
        val today = LocalDate.now()

        val semesterStartDate: LocalDate = try {
            LocalDate.parse(semesterStartDateString, dateFormatter)
        } catch (e: Exception) {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            return
        }

        val alignedSemesterStartDate = getStartDayOfWeek(semesterStartDate, firstDayOfWeekInt)

        val widgetCourses = mutableListOf<WidgetCourse>()
        val startSyncDate = if (today.isBefore(alignedSemesterStartDate)) {
            // 如果开学日期在未来，则从开学日期开始同步
            alignedSemesterStartDate
        } else {
            // 否则从今天开始同步
            today
        }

        for (i in 0 until WIDGET_SYNC_DAYS) {
            val date = startSyncDate.plusDays(i)
            val dateString = date.format(dateFormatter)

            val alignedDate = getStartDayOfWeek(date, firstDayOfWeekInt)

            val diffWeeks = ChronoUnit.WEEKS.between(alignedSemesterStartDate, alignedDate).toInt()
            val weekNumber = diffWeeks + 1

            val dayOfWeek = date.dayOfWeek.value // 1=Monday, 7=Sunday

            if (weekNumber < 1 || weekNumber > semesterTotalWeeks) {
                continue
            }

            for (courseWithWeeks in coursesWithWeeks) {
                if (courseWithWeeks.weeks.any { it.weekNumber == weekNumber } && courseWithWeeks.course.day == dayOfWeek) {
                    val course = courseWithWeeks.course
                    val startTime = timeSlotMap[course.startSection]?.startTime ?: ""
                    val endTime = timeSlotMap[course.endSection]?.endTime ?: ""
                    val isSkipped = skippedDates.contains(dateString)

                    val widgetCourse = WidgetCourse(
                        id = "${course.id}-$dateString",
                        name = course.name,
                        teacher = course.teacher,
                        position = course.position,
                        startTime = startTime,
                        endTime = endTime,
                        isSkipped = isSkipped,
                        date = dateString
                    )
                    widgetCourses.add(widgetCourse)
                }
            }
        }

        widgetRepository.deleteAll()
        if (widgetCourses.isNotEmpty()) {
            widgetRepository.insertAll(widgetCourses)
        }
        updateAllWidgets(appContext)
    }
}