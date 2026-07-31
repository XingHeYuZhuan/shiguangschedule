package com.xingheyuzhuan.shiguangschedule.data.sync

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * 负责主数据库与 Widget 数据库之间的数据同步（跨平台共享核心逻辑）。
 * 持续监听应用设置、课表及时间段的变化，自动计算并写入优化后的 Widget 专用数据库。
 */
@Single
class WidgetDataSynchronizer(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val widgetRepository: WidgetRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val widgetSyncDays = 7 // 每次同步未来 7 天的数据

    // 内部通道：用于向各平台分发“数据同步完成”的通知信号
    private val _syncCompletedChannel = Channel<Unit>(Channel.CONFLATED)

    /** 暴露给各平台（Android / iOS）监听的同步完成事件流 */
    val syncCompletedFlow: Flow<Unit> = _syncCompletedChannel.receiveAsFlow()

    /**
     * 启动自动同步监听（跨平台调用入口）。
     * 会对数据库流的变化进行防抖处理，并在每次同步完成后发出通知。
     */
    @OptIn(FlowPreview::class)
    fun startSync() {
        syncFlow
            .debounce(500.milliseconds)
            .onEach {
                // 通知各平台：数据已同步更新，可以刷新 Widget 或系统通知了
                _syncCompletedChannel.trySend(Unit)
            }
            .launchIn(scope)
    }

    /**
     * 持续监听主数据库变化的 Flow 核心链条。
     * 当当前课表 ID 改变时，会自动切换监听对应的课程、时间段与配置数据。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val syncFlow: Flow<Unit> = appSettingsRepository.getAppSettings()
        .flatMapLatest { appSettings ->
            val tableId = appSettings.currentCourseTableId

            if (tableId.isNotEmpty()) {
                val coursesFlow = courseTableRepository.getCoursesWithWeeksByTableId(tableId)
                val timeSlotsFlow = timeSlotRepository.getTimeSlotsByCourseTableId(tableId)
                val configFlow = appSettingsRepository.getCourseTableConfigFlow(tableId)

                // 联合监听当前课表的所有相关数据表
                combine(coursesFlow, timeSlotsFlow, configFlow) { courses, timeSlots, config ->
                    Quadruple(appSettings, courses, timeSlots, config)
                }
            } else {
                flowOf(Quadruple(appSettings, emptyList(), emptyList(), null))
            }
        }.combine(flowOf(Unit)) { (appSettings, coursesWithWeeks, timeSlots, config), _ ->
            if (config != null) {
                performSync(appSettings, config, coursesWithWeeks, timeSlots)
            } else {
                // 配置为空时清空小组件数据
                widgetRepository.deleteAll()
                widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            }
        }

    /** 四元组辅助数据类，用于 combine 操作符传递多路数据 */
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    /**
     * 手动触发一次性数据同步（挂起函数）。
     */
    suspend fun syncNow() {
        val appSettings = appSettingsRepository.getAppSettings().first()
        val tableId = appSettings.currentCourseTableId

        val coursesWithWeeks = if (tableId.isNotEmpty()) courseTableRepository.getCoursesWithWeeksByTableId(tableId).first() else emptyList()
        val timeSlots = if (tableId.isNotEmpty()) timeSlotRepository.getTimeSlotsByCourseTableId(tableId).first() else emptyList()
        val courseConfig = if (tableId.isNotEmpty()) appSettingsRepository.getCourseConfigOnce(tableId) else null

        if (courseConfig != null) {
            performSync(appSettings, courseConfig, coursesWithWeeks, timeSlots)
        } else {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
        }
        // 手动同步完成后主动发出通知
        _syncCompletedChannel.trySend(Unit)
    }

    /**
     * 核心计算与写库逻辑：解析开学日期、计算周次、匹配课程时间并写入 Widget 数据库。
     */
    private suspend fun performSync(
        appSettings: AppSettingsModel,
        courseConfig: CourseTableConfig,
        coursesWithWeeks: List<CourseWithWeeks>,
        timeSlots: List<TimeSlot>
    ) {
        val semesterStartDateString = courseConfig.semesterStartDate ?: run {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            return
        }
        val semesterTotalWeeks = courseConfig.semesterTotalWeeks
        val firstDayOfWeekInt = courseConfig.firstDayOfWeek

        if (semesterTotalWeeks <= 0) {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            return
        }

        // 更新小组件的全局基础设置
        val widgetSettings = WidgetAppSettings(
            id = 1,
            semesterStartDate = semesterStartDateString,
            semesterTotalWeeks = semesterTotalWeeks,
            firstDayOfWeek = firstDayOfWeekInt
        )
        widgetRepository.insertOrUpdateAppSettings(widgetSettings)

        val skippedDates = appSettings.skippedDates
        val timeSlotMap = timeSlots.associateBy { it.number }
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val semesterStartDate: LocalDate = try {
            LocalDate.parse(semesterStartDateString)
        } catch (e: Exception) {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            return
        }

        // 将开学日期对齐到设定的每周起始日
        val alignedSemesterStartDate = getStartDayOfWeek(semesterStartDate, firstDayOfWeekInt)

        val widgetCourses = mutableListOf<WidgetCourse>()
        // 如果还没开学，从开学第一天开始同步；否则从今天开始同步
        val startSyncDate = if (today < alignedSemesterStartDate) {
            alignedSemesterStartDate
        } else {
            today
        }

        // 循环计算未来指定天数（widgetSyncDays）内的课程安排
        for (i in 0 until widgetSyncDays) {
            val date = startSyncDate.plus(i, DateTimeUnit.DAY)
            val dateString = date.toString()

            val alignedDate = getStartDayOfWeek(date, firstDayOfWeekInt)

            // 计算当前日期是第几周
            val diffDays = alignedSemesterStartDate.daysUntil(alignedDate)
            val diffWeeks = diffDays / 7
            val weekNumber = diffWeeks + 1

            val dayOfWeek = date.dayOfWeek.isoDayNumber

            // 超出学期总周数则跳过
            if (weekNumber !in 1..semesterTotalWeeks) {
                continue
            }

            // 遍历所有课程，检查是否在当前周及当天有课
            for (courseWithWeeks in coursesWithWeeks) {
                if (courseWithWeeks.weeks.any { it.weekNumber == weekNumber } && courseWithWeeks.course.day == dayOfWeek) {
                    val course = courseWithWeeks.course

                    val startTime: String
                    val endTime: String

                    // 处理自定义时间或标准时间段
                    if (course.isCustomTime) {
                        startTime = course.customStartTime ?: ""
                        endTime = course.customEndTime ?: ""
                    } else {
                        startTime = timeSlotMap[course.startSection]?.startTime ?: ""
                        endTime = timeSlotMap[course.endSection]?.endTime ?: ""
                    }

                    // 检查该日期是否被设为调休/停课
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

        // 刷新 Widget 数据库：先清空旧数据，再批量插入新计算的课程
        widgetRepository.deleteAll()
        if (widgetCourses.isNotEmpty()) {
            widgetRepository.insertAll(widgetCourses)
        }
    }

    /**
     * 根据设定的每周起始日（如周一或周日），向前推算并对齐给定日期所在周的起始日。
     */
    private fun getStartDayOfWeek(date: LocalDate, firstDayOfWeekInt: Int): LocalDate {
        val targetFirstDay = DayOfWeek(firstDayOfWeekInt)
        var current = date
        while (current.dayOfWeek != targetFirstDay) {
            current = current.minus(1, DateTimeUnit.DAY)
        }
        return current
    }
}