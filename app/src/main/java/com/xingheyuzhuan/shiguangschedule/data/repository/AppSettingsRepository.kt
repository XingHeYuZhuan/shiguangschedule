package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 应用设置数据仓库，负责处理与应用设置相关的业务逻辑。
 */
class AppSettingsRepository(
    private val appSettingsDao: AppSettingsDao
) {
    /**
     * 默认的应用设置，用于数据库中没有数据时使用。
     * 它的值与 AppSettings 数据类的默认值保持一致。
     * (没办法保证数据库先加载完成再启动ui了)
     */
    private val DEFAULT_SETTINGS = AppSettings(
        id = 1,
        showWeekends = false,
        currentCourseTableId = null,
        semesterStartDate = null,
        semesterTotalWeeks = 20,
        defaultClassDuration = 45,
        defaultBreakDuration = 10,
        reminderEnabled = false,
        remindBeforeMinutes = 15,
        skippedDates = null
    )

    /**
     * 获取应用设置，返回一个数据流。
     * 这是 Repository 对外提供的唯一查询接口。
     * 使用 map 操作符将 Flow<AppSettings?> 转换为 Flow<AppSettings>，
     * 如果数据库为空则提供默认值。
     */
    fun getAppSettings(): Flow<AppSettings> {
        return appSettingsDao.getAppSettings().map { settings ->
            settings ?: DEFAULT_SETTINGS
        }
    }


    /**
     * 【新增】获取一次性的应用设置，用于不需要监听变化的场景。
     * @return 返回 AppSettings 对象，如果找不到则返回 null。
     */
    suspend fun getAppSettingsOnce(): AppSettings? {
        return appSettingsDao.getAppSettings().first()
    }

    /**
     * 实时获取当前周的计算结果，它是一个单次执行的函数。
     */
    suspend fun calculateCurrentWeekFromDb(): Int? {
        val settings = appSettingsDao.getAppSettings().first() ?: DEFAULT_SETTINGS
        return calculateCurrentWeek(settings.semesterStartDate, settings.semesterTotalWeeks)
    }

    /**
     * 根据周数反向推算开学日期，并将新日期写入数据库。
     * 如果传入 null，则将开学日期也设为 null。
     */
    suspend fun setSemesterStartDateFromWeek(week: Int?) {
        val newStartDate = if (week != null) calculateSemesterStartDate(week) else null
        val currentSettings = appSettingsDao.getAppSettings().first() ?: DEFAULT_SETTINGS
        val updatedSettings = currentSettings.copy(semesterStartDate = newStartDate)
        appSettingsDao.insertOrUpdate(updatedSettings)
    }

    /**
     * 插入或更新应用设置。
     * 简化了所有与 currentWeek 相关的联动逻辑。
     */
    suspend fun insertOrUpdateAppSettings(newSettings: AppSettings) {
        val oldSettings = appSettingsDao.getAppSettings().first() ?: DEFAULT_SETTINGS

        val updatedSettings = when {
            // 场景 1: 开学日期被修改，只负责对齐周一
            newSettings.semesterStartDate != oldSettings.semesterStartDate -> {
                val semesterStartDate = newSettings.semesterStartDate?.let { getMondayOfWeek(it) }
                newSettings.copy(semesterStartDate = semesterStartDate)
            }
            // 场景 2: 其他设置修改，无需联动
            else -> newSettings
        }
        appSettingsDao.insertOrUpdate(updatedSettings)
    }

    /**
     * 根据学期开始日期和总周数，计算当前周数。
     * 如果计算出的周数超出 [1, semesterTotalWeeks] 范围，则返回 null。
     */
    private fun calculateCurrentWeek(startDate: String?, totalWeeks: Int): Int? {
        if (startDate.isNullOrEmpty()) return null
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            // 将开学日期对齐到周一
            val startCal = Calendar.getInstance().apply {
                time = dateFormat.parse(startDate)!!
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            // 将当前日期也对齐到周一
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val diffMillis = todayCal.timeInMillis - startCal.timeInMillis
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)
            // 如果开学日期在未来，则返回 null，表示假期
            if (diffDays < 0) {
                return null
            }

            val calculatedWeek = (diffDays / 7).toInt() + 1
            if (calculatedWeek >= 1 && calculatedWeek <= totalWeeks) {
                calculatedWeek
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 根据用户选择的日期，反向推算出该日期所在周的周一日期。
     */
    private fun getMondayOfWeek(dateString: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply { time = dateFormat.parse(dateString)!! }
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return dateFormat.format(calendar.time)
    }

    /**
     * 根据当前周数，反向推算出学期开始日期。
     */
    private fun calculateSemesterStartDate(week: Int): String {
        val todayCal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekInChina = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        val daysToSubtract = (week - 1) * 7 + (dayOfWeekInChina - 1)
        todayCal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        return dateFormat.format(todayCal.time)
    }
}