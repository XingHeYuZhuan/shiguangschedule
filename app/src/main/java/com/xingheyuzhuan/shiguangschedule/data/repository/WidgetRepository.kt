package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettingsDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Widget 数据仓库，负责处理与 Widget 数据库相关的所有数据操作。
 */
class WidgetRepository(
    private val widgetCourseDao: WidgetCourseDao,
    private val widgetAppSettingsDao: WidgetAppSettingsDao,
    private val context: Context
) {
    // 使用线程安全的 java.time.DateTimeFormatter
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    // 创建一个 Channel，用于发送数据更新事件。
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
     * 删除所有课程。
     */
    suspend fun deleteAll() {
        widgetCourseDao.deleteAll()
        _dataUpdatedChannel.trySend(Unit)
    }

    /**
     * 插入或更新小组件设置（WidgetAppSettings）。
     */
    suspend fun insertOrUpdateAppSettings(settings: WidgetAppSettings) {
        widgetAppSettingsDao.insertOrUpdate(settings) // 调用 Dao 的 insertOrUpdate 方法
        _dataUpdatedChannel.trySend(Unit) // 通知监听者数据已更新
    }

    /**
     * 获取小组件设置的数据流。
     * 返回类型已修正为 WidgetAppSettings，匹配 widgetAppSettingsDao 的实际返回。
     */
    fun getAppSettingsFlow(): Flow<WidgetAppSettings?> {
        // 由于 widgetAppSettingsDao 的定义是 getAppSettings(): Flow<WidgetAppSettings?>
        // 这里可以直接返回，不再需要类型转换或 Suppress
        return widgetAppSettingsDao.getAppSettings()
    }

    /**
     * 计算并发出当前周数，它是一个数据流。
     */
    fun getCurrentWeekFlow(): Flow<Int?> {
        // 直接使用 widgetAppSettingsDao 返回正确的 Flow<WidgetAppSettings?> 类型
        return widgetAppSettingsDao.getAppSettings()
            .map { settings ->
                val totalWeeks = settings?.semesterTotalWeeks ?: 0
                val startDate = settings?.semesterStartDate
                val firstDayOfWeek = settings?.firstDayOfWeek ?: DayOfWeek.MONDAY.value

                calculateCurrentWeek(startDate, totalWeeks, firstDayOfWeek)
            }
    }

    // ---------------------------------------------------------------------------------------------
    // 【现代化计算方法：基于 java.time，与主 AppSettingsRepository 保持一致】
    // ---------------------------------------------------------------------------------------------

    /**
     * 根据学期开始日期和总周数，计算当前周数。
     * @param semesterStartDateStr 学期开始日期字符串，格式为 yyyy-MM-dd
     * @param totalWeeks 学期总周数
     * @param firstDayOfWeekInt 一周起始日 (1=MONDAY, 7=SUNDAY)
     * @return 当前周数 (从1开始)，如果不在学期内则返回 null
     */
    private fun calculateCurrentWeek(semesterStartDateStr: String?, totalWeeks: Int, firstDayOfWeekInt: Int): Int? {
        if (semesterStartDateStr.isNullOrEmpty() || totalWeeks <= 0) return null

        return try {
            // 1. 将开学日期对齐到设置的一周起始日
            val alignedStartDateString = getStartDayOfWeek(semesterStartDateStr, firstDayOfWeekInt)
            val alignedStartDate = LocalDate.parse(alignedStartDateString, DATE_FORMATTER)

            // 2. 将当前日期也对齐到设置的一周起始日
            val alignedTodayDateString = getStartDayOfWeek(LocalDate.now().format(DATE_FORMATTER), firstDayOfWeekInt)
            val alignedToday = LocalDate.parse(alignedTodayDateString, DATE_FORMATTER)

            if (alignedToday.isBefore(alignedStartDate)) return null

            // 使用 ChronoUnit 直接计算周数差
            val diffWeeks = ChronoUnit.WEEKS.between(alignedStartDate, alignedToday).toInt()
            val calculatedWeek = diffWeeks + 1

            if (calculatedWeek in 1..totalWeeks) calculatedWeek else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 根据日期和设置的一周起始日，反向推算出该日期所在周的起始日。
     */
    private fun getStartDayOfWeek(dateString: String, firstDayOfWeekInt: Int): String {
        val date = LocalDate.parse(dateString, DATE_FORMATTER)
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)
        val adjustedDate = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return adjustedDate.format(DATE_FORMATTER)
    }
}