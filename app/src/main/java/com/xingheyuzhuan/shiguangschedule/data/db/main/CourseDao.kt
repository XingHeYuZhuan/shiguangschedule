package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.RoomWarnings
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作课程 (Course) 数据表。
 */
@Dao
interface CourseDao {
    /**
     * 获取指定课表ID的所有课程。
     * 返回一个 Flow，当数据变化时会自动更新。
     */
    @Query("SELECT * FROM courses WHERE courseTableId = :courseTableId ORDER BY day ASC, startSection ASC, endSection ASC")
    fun getCoursesByTableId(courseTableId: String): Flow<List<Course>>

    /**
     * 获取指定课表ID的所有课程，并包含其对应的周数。
     * @Transaction 注解确保这是一个原子操作，以避免数据不一致。
     * Room 会自动根据 CourseWithWeeks 中的 @Relation 注解执行关联查询。
     */
    @Transaction
    @Query("SELECT * FROM courses WHERE courseTableId = :courseTableId ORDER BY day ASC, startSection ASC, endSection ASC")
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun getCoursesWithWeeksByTableId(courseTableId: String): Flow<List<CourseWithWeeks>>

    /**
     * 插入一个或多个课程。如果发生主键冲突，则替换旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<Course>)

    /**
     * 删除一个或多个课程。
     */
    @Delete
    suspend fun delete(course: Course)

    /**
     * 根据课程ID删除单个课程。
     */
    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteById(courseId: String)

    /**
     * 删除指定课表ID下的所有课程。
     * 这是支持覆盖导入功能的关键方法。
     */
    @Query("DELETE FROM courses WHERE courseTableId = :courseTableId")
    suspend fun deleteCoursesByTableId(courseTableId: String)

    /**
     * 获取指定课表ID下，在特定星期和周次的所有课程及其周数。
     *
     * @param courseTableId 课表ID。
     * @param day 星期几。
     * @param weekNumber 周次。
     */
    @Transaction
    @Query(
        """
    SELECT * FROM courses AS c
    INNER JOIN course_weeks AS cw ON c.id = cw.courseId
    WHERE c.courseTableId = :courseTableId
      AND c.day = :day
      AND cw.weekNumber = :weekNumber
    """
    )
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun getCoursesWithWeeksByDayAndWeek(
        courseTableId: String,
        day: Int,
        weekNumber: Int
    ): Flow<List<CourseWithWeeks>>

    /**
     * 专门用于根据课程 ID 更新 colorInt 字段。
     * 用于将旧的 ARGB 值迁移到新的索引值。
     */
    @Query("UPDATE courses SET colorInt = :newColorInt WHERE id = :courseId")
    suspend fun updateCourseColorById(courseId: String, newColorInt: Int)
}