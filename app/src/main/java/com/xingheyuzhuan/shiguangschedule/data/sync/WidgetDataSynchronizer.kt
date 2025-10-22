package com.xingheyuzhuan.shiguangschedule.data.sync

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 负责主数据库和 Widget 数据库之间的数据同步。
 * 它持续监听数据变化，并自动将数据处理后存入为 Widget 优化的数据库。
 * 注意：这个类本身不启动监听，启动任务由外部的 SyncManager 负责。
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
     * 一个持续发出 Unit 的 Flow，外部只需收集这个 Flow 即可触发同步。
     * 适合在前台运行时，对主数据库的数据变化做出即时响应。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val syncFlow: Flow<Unit> = appSettingsRepository.getAppSettings()
        .flatMapLatest { appSettings ->
            val tableId = appSettings?.currentCourseTableId

            if (tableId != null) {
                combine(
                    courseTableRepository.getCoursesWithWeeksByTableId(tableId),
                    timeSlotRepository.getTimeSlotsByCourseTableId(tableId)
                ) { courses, timeSlots ->
                    Triple(appSettings, courses, timeSlots)
                }
            } else {
                flowOf(Triple(appSettings, emptyList(), emptyList()))
            }
        }.combine(flowOf(Unit)) { (appSettings, coursesWithWeeks, timeSlots), _ ->
            // 将实际同步逻辑提取到单独的方法中
            if (appSettings != null) {
                performSync(appSettings, coursesWithWeeks, timeSlots)
            }
        }

    /**
     * 这是一个公共的挂起函数，用于手动触发一次性数据同步。
     * 适合在后台任务（如 WorkManager）中调用。
     */
    suspend fun syncNow() {
        val appSettings = appSettingsRepository.getAppSettings().first() // 获取最新设置
        val tableId = appSettings?.currentCourseTableId
        val coursesWithWeeks = if (tableId != null) courseTableRepository.getCoursesWithWeeksByTableId(tableId).first() else emptyList()
        val timeSlots = if (tableId != null) timeSlotRepository.getTimeSlotsByCourseTableId(tableId).first() else emptyList()

        if (appSettings != null) {
            performSync(appSettings, coursesWithWeeks, timeSlots)
        }
    }

    /**
     * 实际执行同步逻辑的私有方法，避免代码重复。
     */
    private suspend fun performSync(
        appSettings: AppSettings,
        coursesWithWeeks: List<CourseWithWeeks>,
        timeSlots: List<TimeSlot>
    ) {
        val currentTableId = appSettings.currentCourseTableId ?: run {
            widgetRepository.deleteAll()
            return
        }
        val semesterStartDateString = appSettings.semesterStartDate ?: run {
            widgetRepository.deleteAll()
            return
        }
        val semesterTotalWeeks = appSettings.semesterTotalWeeks

        val skippedDates = appSettings.skippedDates ?: emptySet()
        val timeSlotMap = timeSlots.associateBy { it.number }
        val today = LocalDate.now()

        val semesterStartDate: LocalDate = try {
            LocalDate.parse(semesterStartDateString, dateFormatter)
        } catch (e: Exception) {
            widgetRepository.deleteAll()
            return
        }

        val widgetCourses = mutableListOf<WidgetCourse>()
        val startSyncDate = if (today.isBefore(semesterStartDate)) {
            semesterStartDate
        } else {
            today
        }

        for (i in 0 until WIDGET_SYNC_DAYS) {
            val date = startSyncDate.plusDays(i)
            val dateString = date.format(dateFormatter)

            val daysSinceSemesterStart = ChronoUnit.DAYS.between(semesterStartDate, date)
            val weekNumber = (daysSinceSemesterStart / 7).toInt() + 1
            val dayOfWeek = date.dayOfWeek.value

            if (weekNumber < 1 || semesterTotalWeeks <= 0 || weekNumber > semesterTotalWeeks) {
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
                        date = dateString,
                        colorInt = course.colorInt
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
