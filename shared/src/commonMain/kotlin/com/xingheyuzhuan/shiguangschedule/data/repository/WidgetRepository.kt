package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettingsDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourseDao
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Widget 数据仓库，负责处理与 Widget 数据库相关的所有数据操作。
 */
@Single
class WidgetRepository(
    private val widgetCourseDao: WidgetCourseDao,
    private val widgetAppSettingsDao: WidgetAppSettingsDao
) {

    private val _dataUpdatedChannel = Channel<Unit>(Channel.CONFLATED)
    val dataUpdatedFlow: Flow<Unit> = _dataUpdatedChannel.receiveAsFlow()

    /**
     * 获取指定日期范围内的 Widget 课程。
     */
    fun getWidgetCoursesByDateRange(startDate: String, endDate: String): Flow<List<WidgetCourse>> {
        return widgetCourseDao.getWidgetCoursesByDateRange(startDate, endDate)
    }

    /**
     * 批量插入或更新 Widget 课程。
     */
    suspend fun insertAll(courses: List<WidgetCourse>) {
        widgetCourseDao.insertAll(courses)
        _dataUpdatedChannel.trySend(Unit)
    }

    /**
     * 删除所有 Widget 课程。
     */
    suspend fun deleteAll() {
        widgetCourseDao.deleteAll()
        _dataUpdatedChannel.trySend(Unit)
    }

    /**
     * 插入或更新小组件设置。
     */
    suspend fun insertOrUpdateAppSettings(settings: WidgetAppSettings) {
        widgetAppSettingsDao.insertOrUpdate(settings)
        _dataUpdatedChannel.trySend(Unit)
    }

    /**
     * 获取小组件设置的数据流。
     */
    fun getAppSettingsFlow(): Flow<WidgetAppSettings?> {
        return widgetAppSettingsDao.getAppSettings()
    }

    /**
     * 获取当前学期周数的数据流。
     */
    fun getCurrentWeekFlow(): Flow<Int?> {
        return widgetAppSettingsDao.getAppSettings()
            .map { settings ->
                val totalWeeks = settings?.semesterTotalWeeks ?: 0
                val startDate = settings?.semesterStartDate
                val firstDayOfWeek = settings?.firstDayOfWeek ?: DayOfWeek.MONDAY.isoDayNumber

                calculateCurrentWeek(startDate, totalWeeks, firstDayOfWeek)
            }
    }

    /**
     * 根据学期开始日期和总周数计算当前周数。
     *
     * @param semesterStartDateStr 学期开始日期字符串，格式为 yyyy-MM-dd
     * @param totalWeeks 学期总周数
     * @param firstDayOfWeekInt 一周起始日 (1=MONDAY, 7=SUNDAY)
     * @return 当前周数 (从1开始)，若未开始或已结束则返回 null
     */
    private fun calculateCurrentWeek(semesterStartDateStr: String?, totalWeeks: Int, firstDayOfWeekInt: Int): Int? {
        if (semesterStartDateStr.isNullOrEmpty() || totalWeeks <= 0) return null

        return try {
            val startDate = LocalDate.parse(semesterStartDateStr)
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

            val alignedStartDate = getStartDayOfWeek(startDate, firstDayOfWeekInt)
            val alignedToday = getStartDayOfWeek(today, firstDayOfWeekInt)

            if (alignedToday < alignedStartDate) return null

            val diffDays = alignedStartDate.daysUntil(alignedToday)
            val diffWeeks = diffDays / 7
            val calculatedWeek = diffWeeks + 1

            if (calculatedWeek in 1..totalWeeks) calculatedWeek else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 计算指定日期所在周的起始日期。
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