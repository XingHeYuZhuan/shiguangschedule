package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作课程周数 (CourseWeek) 关联数据表。
 */
@Dao
interface CourseWeekDao {
    /**
     * 获取指定课程的所有周数。
     */
    @Query("SELECT * FROM course_weeks WHERE courseId = :courseId ORDER BY weekNumber ASC")
    fun getWeeksByCourseId(courseId: String): Flow<List<CourseWeek>>

    /**
     * 批量插入课程周数。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courseWeeks: List<CourseWeek>)

    /**
     * 更新指定课程的周数列表。
     * 这个方法在一个事务中执行，确保原子操作。
     * 它会先删除该课程所有的旧周数，然后插入新的周数。
     */
    @Transaction
    suspend fun updateCourseWeeks(courseId: String, courseWeeks: List<CourseWeek>) {
        deleteByCourseId(courseId)
        insertAll(courseWeeks)
    }

    /**
     * 根据课程ID删除其所有的周数记录。
     * 这是 updateCourseWeeks 内部调用的私有方法。
     */
    @Query("DELETE FROM course_weeks WHERE courseId = :courseId")
    suspend fun deleteByCourseId(courseId: String)
    /**
     * 根据课程ID列表和周次，批量删除周次记录。
     * 这是实现高效调课的关键方法。
     */
    @Query("DELETE FROM course_weeks WHERE courseId IN (:courseIds) AND weekNumber = :weekNumber")
    suspend fun deleteCourseWeeksForCourseAndWeek(courseIds: List<String>, weekNumber: Int)
}