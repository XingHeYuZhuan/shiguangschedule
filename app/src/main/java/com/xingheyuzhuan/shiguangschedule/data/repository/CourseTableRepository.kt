package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.room.Transaction
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeekDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课表数据仓库，负责处理所有与课表、课程相关的业务逻辑和数据操作。
 */
@Singleton
class CourseTableRepository @Inject constructor(
    private val courseTableDao: CourseTableDao,
    private val courseDao: CourseDao,
    private val courseWeekDao: CourseWeekDao,
    private val timeSlotRepository: TimeSlotRepository,
    private val appSettingsRepository: AppSettingsRepository
) {
    /**
     * 获取所有课表，返回一个数据流。
     */
    fun getAllCourseTables(): Flow<List<CourseTable>> {
        return courseTableDao.getAllCourseTables()
    }

    /**
     * 获取指定课表ID的完整课程（包含周数）。
     */
    fun getCoursesWithWeeksByTableId(tableId: String): Flow<List<CourseWithWeeks>> {
        return courseDao.getCoursesWithWeeksByTableId(tableId)
    }

    /**
     * 创建一个新的课表。
     * 负责生成 ID 并执行插入操作，并**同步**为新课表创建默认时间段和配置。
     *
     * @param name 新课表的名称
     */
    @Transaction
    suspend fun createNewCourseTable(name: String) {
        val newTable = CourseTable(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis()
        )
        courseTableDao.insert(newTable)

        // 2. 插入默认时间段
        val defaultTimeSlotsForNewTable = defaultTimeSlots.map {
            it.copy(courseTableId = newTable.id)
        }
        // 调用 timeSlotRepository 的方法来插入时间段
        timeSlotRepository.insertAll(defaultTimeSlotsForNewTable)

        // 3. 插入默认课表配置
        val newConfig = CourseTableConfig(courseTableId = newTable.id)
        appSettingsRepository.insertOrUpdateCourseConfig(newConfig)
    }

    /**
     * 更新一个课表。
     */
    suspend fun updateCourseTable(courseTable: CourseTable) {
        courseTableDao.update(courseTable)
    }

    /**
     * 删除一个课表，并确保至少保留一个。
     *
     * @return 如果删除成功返回 true，否则返回 false。
     */
    suspend fun deleteCourseTable(courseTable: CourseTable): Boolean {
        val allTables = courseTableDao.getAllCourseTables().first()
        if (allTables.size <= 1) {
            return false
        }
        courseTableDao.delete(courseTable)
        return true
    }

    /**
     * 专门用于根据课程ID更新其颜色索引。
     * 这是实现无效颜色自动修复机制所需的关键方法（用于历史数据迁移）。
     *
     * @param courseId 课程的唯一ID。
     * @param newColorInt 新的颜色索引值 (0 到 11)。
     */
    suspend fun updateCourseColor(courseId: String, newColorInt: Int) {
        courseDao.updateCourseColorById(courseId, newColorInt)
    }

    /**
     * 插入或更新一个课程，并同时更新其对应的周数列表。
     * 通过先检查是否存在，决定使用 @Update 还是 @Insert，
     * 从而在更新课程属性时保护 course_weeks 表不被级联删除误伤。
     */
    @Transaction
    suspend fun upsertCourse(course: Course, weekNumbers: List<Int>) {
        // 逻辑判断：如果数据库里已经有这个 ID
        if (courseDao.exists(course.id)) {
            // 使用 @Update 精准修改字段。
            courseDao.update(course)
        } else {
            // 如果是新 ID，则执行插入。
            courseDao.insertAll(listOf(course))
        }

        // 更新周次关联
        val courseWeeks = weekNumbers.map { week ->
            CourseWeek(courseId = course.id, weekNumber = week)
        }
        courseWeekDao.updateCourseWeeks(course.id, courseWeeks)
    }

    /**
     * 删除一个课程。
     */
    suspend fun deleteCourse(course: Course) {
        courseDao.delete(course)
    }

    /**
     * 批量删除指定 ID 列表的课程实例。
     *
     * @param courseIds 要删除的课程的唯一ID列表。
     */
    suspend fun deleteCoursesByIds(courseIds: List<String>) {
        if (courseIds.isEmpty()) return
        courseDao.deleteCoursesByIds(courseIds)
    }

    /**
     * 批量删除指定课表下、指定名称的所有课程实例及其关联的周次记录。
     *
     * 依赖 Room 的 ForeignKey.CASCADE (在 CourseWeek 实体中定义)，
     * 此方法只需删除 Course 记录，CourseWeek 记录将自动被清理。
     *
     * @param tableId 课表的唯一ID。
     * @param courseNames 需要删除的课程名称列表。
     */
    suspend fun deleteCoursesByNames(tableId: String, courseNames: List<String>) {
        if (courseNames.isEmpty() || tableId.isBlank()) return
        courseDao.deleteCoursesByNames(tableId, courseNames)
    }

    // 在类内部定义枚举
    enum class TweakMode {
        MERGE,      // 数据合并：A -> B (单箭头)
        OVERWRITE,  // 数据覆盖：A >> B (双箭头)
        EXCHANGE    // 数据交换：A <-> B (双向箭头)
    }

    /**
     * 执行调课操作
     * @param mode 传入 TweakMode.MERGE, TweakMode.OVERWRITE 或 TweakMode.EXCHANGE
     */
    @Transaction
    suspend fun tweakCoursesOnDate(
        mode: TweakMode,
        courseTableId: String,
        fromWeek: Int,
        fromDay: Int,
        toWeek: Int,
        toDay: Int
    ) {
        // 1. 获取来源(A)和目标(B)的数据快照
        val sourceList = courseDao.getCoursesWithWeeksByDayAndWeek(courseTableId, fromDay, fromWeek).first()
        val targetList = courseDao.getCoursesWithWeeksByDayAndWeek(courseTableId, toDay, toWeek).first()

        when (mode) {
            TweakMode.MERGE -> {
                // 直接移动 A -> B
                executeMoveInternal(sourceList, toWeek, toDay, fromWeek)
            }

            TweakMode.OVERWRITE -> {
                // 先删目标日期的周次，再移动 A -> B
                if (targetList.isNotEmpty()) {
                    val targetIds = targetList.map { it.course.id }
                    courseWeekDao.deleteCourseWeeksForCourseAndWeek(targetIds, toWeek)
                }
                executeMoveInternal(sourceList, toWeek, toDay, fromWeek)
            }

            TweakMode.EXCHANGE -> {
                // 互换：先切断双方现有周次联系
                if (sourceList.isNotEmpty()) {
                    courseWeekDao.deleteCourseWeeksForCourseAndWeek(sourceList.map { it.course.id }, fromWeek)
                }
                if (targetList.isNotEmpty()) {
                    courseWeekDao.deleteCourseWeeksForCourseAndWeek(targetList.map { it.course.id }, toWeek)
                }
                // 执行交叉移动 (originalWeek = -1 表示不重复执行删除逻辑)
                executeMoveInternal(sourceList, toWeek, toDay, -1)
                executeMoveInternal(targetList, fromWeek, fromDay, -1)
            }
        }
    }

    /**
     * 内部辅助函数：处理课程的物理移动或多周拆分逻辑
     */
    private suspend fun executeMoveInternal(
        items: List<CourseWithWeeks>,
        targetWeek: Int,
        targetDay: Int,
        originalWeek: Int
    ) {
        if (items.isEmpty()) return

        val newCourses = mutableListOf<Course>()
        val newWeeks = mutableListOf<CourseWeek>()
        val oldIdsToRemove = mutableListOf<String>()

        for (item in items) {
            val course = item.course
            // 判断是否为单周课程
            val isSingleWeek = item.weeks.size <= 1

            if (isSingleWeek) {
                // 优化：只有一周的课，直接更新 Course 表的 Day 字段
                courseDao.update(course.copy(day = targetDay))
                if (originalWeek != -1) {
                    courseWeekDao.deleteCourseWeeksForCourseAndWeek(listOf(course.id), originalWeek)
                }
                courseWeekDao.insertAll(listOf(CourseWeek(courseId = course.id, weekNumber = targetWeek)))
            } else {
                // 优化：多周课，克隆新 ID 专门用于目标日期
                val newId = UUID.randomUUID().toString()
                newCourses.add(course.copy(id = newId, day = targetDay))
                newWeeks.add(CourseWeek(courseId = newId, weekNumber = targetWeek))
                if (originalWeek != -1) oldIdsToRemove.add(course.id)
            }
        }

        // 批量执行数据库变更
        if (oldIdsToRemove.isNotEmpty()) {
            courseWeekDao.deleteCourseWeeksForCourseAndWeek(oldIdsToRemove, originalWeek)
        }
        if (newCourses.isNotEmpty()) {
            courseDao.insertAll(newCourses)
            courseWeekDao.insertAll(newWeeks)
        }
    }

    /**
     * 快速删除：仅移除特定周次的记录，不物理删除课程定义。
     * * @param tableId 课表唯一 ID
     * @param weekDayPairs 周次与星期的组合列表 (Pair<周次, 星期>)
     */
    @Transaction
    suspend fun deleteCoursesOnDates(
        tableId: String,
        weekDayPairs: List<Pair<Int, Int>>
    ) {
        weekDayPairs.forEach { (week, day) ->
            // 查找在该课表、该周、该天下的所有课程记录
            val coursesToDelete = getCoursesForDay(tableId, week, day).first()

            if (coursesToDelete.isNotEmpty()) {
                val ids = coursesToDelete.map { it.course.id }

                // 仅仅从 course_weeks 表中移除对应周次的关联，不触动 course 表
                courseWeekDao.deleteCourseWeeksForCourseAndWeek(ids, week)
            }
        }
    }

    /**
     * 获取指定课表、周次和星期下的课程，并以数据流形式返回。
     * 这个方法专为 UI 层提供实时更新的数据。
     */
    fun getCoursesForDay(
        courseTableId: String,
        weekNumber: Int,
        day: Int
    ): Flow<List<CourseWithWeeks>> {
        // 直接调用底层的 DAO 方法
        return courseDao.getCoursesWithWeeksByDayAndWeek(
            courseTableId = courseTableId,
            day = day,
            weekNumber = weekNumber
        )
    }

    /**
     * 根据物理日期和配置，获取该周的所有课程。
     * 此函数是重构“真日历”模式的关键，它实现了从“日期”到“课程数据”的直接映射。
     */
    fun getCoursesWithWeeksByDate(
        courseTableId: String,
        targetDate: LocalDate,
        config: CourseTableConfig
    ): Flow<List<CourseWithWeeks>> {
        val weekNumber = appSettingsRepository.getWeekIndexAtDate(
            targetDate = targetDate,
            startDateStr = config.semesterStartDate,
            firstDayOfWeekInt = config.firstDayOfWeek
        )

        // 如果周次为 null（说明没设开学日期）
        if (weekNumber == null) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        // 3. 调用 DAO 层的精准查询方法（按周次过滤）
        return courseDao.getCoursesWithWeeksByTableAndWeek(courseTableId, weekNumber)
    }
}

private val defaultTimeSlots = listOf(
    TimeSlot(number = 1, startTime = "08:00", endTime = "08:45", courseTableId = "placeholder"),
    TimeSlot(number = 2, startTime = "08:50", endTime = "09:35", courseTableId = "placeholder"),
    TimeSlot(number = 3, startTime = "09:50", endTime = "10:35", courseTableId = "placeholder"),
    TimeSlot(number = 4, startTime = "10:40", endTime = "11:25", courseTableId = "placeholder"),
    TimeSlot(number = 5, startTime = "11:30", endTime = "12:15", courseTableId = "placeholder"),
    TimeSlot(number = 6, startTime = "14:00", endTime = "14:45", courseTableId = "placeholder"),
    TimeSlot(number = 7, startTime = "14:50", endTime = "15:35", courseTableId = "placeholder"),
    TimeSlot(number = 8, startTime = "15:45", endTime = "16:30", courseTableId = "placeholder"),
    TimeSlot(number = 9, startTime = "16:35", endTime = "17:20", courseTableId = "placeholder"),
    TimeSlot(number = 10, startTime = "18:30", endTime = "19:15", courseTableId = "placeholder"),
    TimeSlot(number = 11, startTime = "19:20", endTime = "20:05", courseTableId = "placeholder"),
    TimeSlot(number = 12, startTime = "20:10", endTime = "20:55", courseTableId = "placeholder"),
    TimeSlot(number = 13, startTime = "21:10", endTime = "21:55", courseTableId = "placeholder")
)