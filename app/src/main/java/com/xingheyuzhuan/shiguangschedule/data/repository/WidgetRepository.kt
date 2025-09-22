package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettingsDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourseDao
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Widget 数据仓库，负责处理与 Widget 数据库相关的所有数据操作。
 * 它封装了 WidgetCourseDao 和 AppSettingsDao，为上层业务逻辑提供高层次的接口。
 */
class WidgetRepository(
    private val widgetCourseDao: WidgetCourseDao,
    private val widgetAppSettingsDao: AppSettingsDao,
    private val context: Context
) {
    // 创建一个 Channel，用于发送数据更新事件。
    // 容量为 Channel.CONFLATED 代表如果发送者速度快于接收者，只会保留最新的数据。
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
     * 在同步前，我们可以清空旧数据。
     */
    suspend fun deleteAll() {
        widgetCourseDao.deleteAll()
        _dataUpdatedChannel.trySend(Unit)
    }

    /**
     * 获取小组件设置的数据流。
     * 这将允许 UI 监听设置变化并自动更新。
     */
    fun getAppSettingsFlow(): Flow<com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettings?> {
        return widgetAppSettingsDao.getAppSettings()
    }

    /**
     * 计算并发出当前周数，它是一个数据流。
     */
    fun getCurrentWeekFlow(): Flow<Int?> {
        return widgetAppSettingsDao.getAppSettings()
            .map { settings ->
                val totalWeeks = settings?.semesterTotalWeeks ?: 0
                val startDate = settings?.semesterStartDate
                calculateCurrentWeek(startDate, totalWeeks)
            }
    }

    /**
     * 根据学期开始日期计算当前周数。
     * @param semesterStartDateStr 学期开始日期字符串，格式为 yyyy-MM-dd
     * @param totalWeeks 学期总周数
     * @return 当前周数 (从1开始)，如果不在学期内则返回 null
     */
    private fun calculateCurrentWeek(semesterStartDateStr: String?, totalWeeks: Int): Int? {
        if (semesterStartDateStr == null) {
            return null
        }
        return try {
            val semesterStartDate = LocalDate.parse(semesterStartDateStr)
            val currentDate = LocalDate.now()
            val daysSinceSemesterStart = ChronoUnit.DAYS.between(semesterStartDate, currentDate)

            if (daysSinceSemesterStart < 0) {
                return null // 学期尚未开始，返回假期状态
            }

            val calculatedWeek = (daysSinceSemesterStart / 7).toInt() + 1
            if (calculatedWeek in 1..totalWeeks) {
                calculatedWeek
            } else {
                null // 已过学期总周数，返回假期状态
            }
        } catch (e: Exception) {
            null // 日期解析失败
        }
    }
}
