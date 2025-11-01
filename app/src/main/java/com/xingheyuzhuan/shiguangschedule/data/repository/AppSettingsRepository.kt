package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.main.AppSettingsDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfigDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 应用设置数据仓库，负责处理与应用设置相关的业务逻辑。
 */
class AppSettingsRepository(
    private val appSettingsDao: AppSettingsDao,
    private val courseTableConfigDao: CourseTableConfigDao
) {
    // 使用线程安全的 java.time.DateTimeFormatter
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 默认的应用设置
     */
    private val DEFAULT_SETTINGS = AppSettings(
        id = 1,
        currentCourseTableId = null,
        reminderEnabled = false,
        remindBeforeMinutes = 15,
        skippedDates = null,
        autoModeEnabled = false,
        autoControlMode = "DND",
    )

    /**
     * 默认的课表配置，用于初始化或找不到配置时的回退
     */
    private val DEFAULT_COURSE_CONFIG = CourseTableConfig(
        courseTableId = UUID.randomUUID().toString(),
        showWeekends = false,
        semesterStartDate = null,
        semesterTotalWeeks = 20,
        defaultClassDuration = 45,
        defaultBreakDuration = 10,
        firstDayOfWeek = DayOfWeek.MONDAY.value
    )


    /**
     * 获取应用设置（全局设置），返回一个数据流。
     */
    fun getAppSettings(): Flow<AppSettings> {
        return appSettingsDao.getAppSettings().map { settings ->
            settings ?: DEFAULT_SETTINGS
        }
    }

    /**
     * 【新增函数】获取一次性的应用设置，用于不需要监听变化的场景。
     * @return 返回 AppSettings 对象，如果找不到则返回 null。
     */
    suspend fun getAppSettingsOnce(): AppSettings? {
        return appSettingsDao.getAppSettings().first()
    }

    /**
     * 【新增函数】根据课表ID，获取该课表的配置信息（一次性）。
     * 此函数用于不需要监听配置变化，只需要获取当前快照的场景。
     */
    suspend fun getCourseConfigOnce(tableId: String): CourseTableConfig? {
        return courseTableConfigDao.getConfigOnce(tableId)
    }

    /**
     * 【新增函数】根据课表 ID，实时获取该课表的配置信息，返回一个数据流。
     * 此函数专为需要监听配置变化（如 ViewModel 中的 combine 和 flatMapLatest）的场景设计。
     * * @param courseTableId 关联的课表 ID
     */
    fun getCourseTableConfigFlow(courseTableId: String): Flow<CourseTableConfig?> {
        // 假设 CourseTableConfigDao 中返回 Flow 的方法名为 getConfigById
        return courseTableConfigDao.getConfigById(courseTableId)
    }

    /**
     * 实时获取当前周的计算结果，它是一个单次执行的函数。
     * 逻辑：根据 AppSettings 中的 currentCourseTableId 查询 CourseTableConfig 进行计算。
     */
    suspend fun calculateCurrentWeekFromDb(): Int? {
        val appSettings = appSettingsDao.getAppSettings().first() ?: return null
        val currentCourseId = appSettings.currentCourseTableId ?: return null

        // 1. 从新的配置表中获取当前课表的设置
        val config = courseTableConfigDao.getConfigOnce(currentCourseId) ?: return null

        return calculateCurrentWeek(
            config.semesterStartDate,
            config.semesterTotalWeeks,
            config.firstDayOfWeek
        )
    }

    /**
     * 根据周数反向推算开学日期，并将新日期写入 CourseTableConfig 数据库。
     */
    suspend fun setSemesterStartDateFromWeek(week: Int?) {
        val appSettings = appSettingsDao.getAppSettings().first() ?: return
        val currentCourseId = appSettings.currentCourseTableId ?: return

        // 1. 获取当前课表的配置
        val currentConfig = courseTableConfigDao.getConfigOnce(currentCourseId)
            ?: DEFAULT_COURSE_CONFIG.copy(courseTableId = currentCourseId)

        val newStartDate = if (week != null) {
            calculateSemesterStartDate(week, currentConfig.firstDayOfWeek)
        } else {
            null
        }

        // 2. 更新 CourseTableConfig
        val updatedConfig = currentConfig.copy(semesterStartDate = newStartDate)
        courseTableConfigDao.insertOrUpdate(updatedConfig)
    }

    /**
     * 插入或更新应用设置（仅限全局设置）。
     * 【重要：该函数不再处理周数相关的字段】
     */
    suspend fun insertOrUpdateAppSettings(newSettings: AppSettings) {
        // AppSettings 现在只包含全局设置，直接更新即可
        appSettingsDao.insertOrUpdate(newSettings)
    }

    /**
     * 【新增函数】插入或更新课表配置。
     */
    suspend fun insertOrUpdateCourseConfig(newConfig: CourseTableConfig) {
        courseTableConfigDao.insertOrUpdate(newConfig)
    }


    // ---------------------------------------------------------------------------------------------
    // 【现代化计算方法：基于 java.time】
    // ---------------------------------------------------------------------------------------------

    /**
     * 根据学期开始日期和总周数，计算当前周数。
     * @param firstDayOfWeekInt 一周起始日 (1=MONDAY, 7=SUNDAY)
     */
    private fun calculateCurrentWeek(startDate: String?, totalWeeks: Int, firstDayOfWeekInt: Int): Int? {
        if (startDate.isNullOrEmpty()) return null

        return try {
            // 1. 将开学日期对齐到设置的一周起始日
            val alignedStartDateString = getStartDayOfWeek(startDate!!, firstDayOfWeekInt)
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
     * 使用 TemporalAdjusters.previousOrSame 完美封装旧逻辑。
     */
    private fun getStartDayOfWeek(dateString: String, firstDayOfWeekInt: Int): String {
        val date = LocalDate.parse(dateString, DATE_FORMATTER)
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)
        // 核心代码：调整到最近的、设置的周起始日（包括自身）
        val adjustedDate = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return adjustedDate.format(DATE_FORMATTER)
    }

    /**
     * 根据当前周数，反向推算出学期开始日期。
     */
    private fun calculateSemesterStartDate(week: Int, firstDayOfWeekInt: Int): String {
        val today = LocalDate.now()
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)

        // 1. 确定当前日期所在周的起始日
        val startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

        // 2. 需要倒退的周数：当前是第 W 周，要回到第 1 周，减去 W-1 周。
        val weeksToSubtract = (week - 1).toLong()

        // 核心代码：直接减去周数
        val semesterStartDate = startOfThisWeek.minusWeeks(weeksToSubtract)

        return semesterStartDate.format(DATE_FORMATTER)
    }
}