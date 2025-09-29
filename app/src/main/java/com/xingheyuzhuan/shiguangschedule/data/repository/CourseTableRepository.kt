package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.room.Transaction
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeekDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * 课表数据仓库，负责处理所有与课表、课程相关的业务逻辑和数据操作。
 * 它封装了底层 DAO，为 ViewModel 提供高层次的业务接口。
 */
class CourseTableRepository(
    private val courseTableDao: CourseTableDao,
    private val courseDao: CourseDao,
    private val courseWeekDao: CourseWeekDao,
    private val timeSlotRepository: TimeSlotRepository
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
     * 负责生成 ID 并执行插入操作，并**同步**为新课表创建默认时间段。
     *
     * @param name 新课表的名称
     */
    suspend fun createNewCourseTable(name: String) {
        val newTable = CourseTable(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis()
        )
        courseTableDao.insert(newTable)

        val defaultTimeSlotsForNewTable = defaultTimeSlots.map {
            it.copy(courseTableId = newTable.id)
        }
        // 调用 timeSlotRepository 的方法来插入时间段
        timeSlotRepository.insertAll(defaultTimeSlotsForNewTable)
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
     * 插入或更新一个课程，并同时更新其对应的周数列表。
     */
    suspend fun upsertCourse(course: Course, weekNumbers: List<Int>) {
        courseDao.insertAll(listOf(course))
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
     * 将指定日期（由星期和周次确定）下的所有课程调动到新日期。
     * 这是一个原子操作，确保数据一致性。
     *
     * @param courseTableId 用户选择的课表ID。
     * @param fromWeekNumber 被移动的周次。
     * @param fromDay 被移动的星期。
     * @param toWeekNumber 移动到的周次。
     * @param toDay 移动到的星期。
     */
    @Transaction
    suspend fun moveCoursesOnDate(
        courseTableId: String,
        fromWeekNumber: Int,
        fromDay: Int,
        toWeekNumber: Int,
        toDay: Int
    ) {
        // 1. 获取所有待移动的课程
        val coursesWithWeeksToMove = courseDao.getCoursesWithWeeksByDayAndWeek(
            courseTableId = courseTableId,
            day = fromDay,
            weekNumber = fromWeekNumber
        ).first()

        // 2. 遍历并处理每一门课程
        for (courseWithWeeks in coursesWithWeeksToMove) {
            val originalCourse = courseWithWeeks.course
            val originalWeeks = courseWithWeeks.weeks.map { it.weekNumber }.toMutableList()

            // 2a. 从原课程中移除被移动的周次
            originalWeeks.remove(fromWeekNumber)
            // 使用 courseWeekDao 的 updateCourseWeeks 方法更新周次
            courseWeekDao.updateCourseWeeks(originalCourse.id, originalWeeks.map {
                CourseWeek(courseId = originalCourse.id, weekNumber = it)
            })

            // 2b. 创建新的课程和周次记录
            val newCourse = originalCourse.copy(
                id = UUID.randomUUID().toString(), // 新的唯一 ID
                day = toDay // 设置为移动到的星期
            )
            val newCourseWeek = CourseWeek(courseId = newCourse.id, weekNumber = toWeekNumber)

            // 2c. 插入新的课程和周次
            courseDao.insertAll(listOf(newCourse))
            courseWeekDao.insertAll(listOf(newCourseWeek))
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