package com.xingheyuzhuan.shiguangschedule.data.repository

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