package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作 WidgetCourse 数据表。
 */
@Dao
interface WidgetCourseDao {
    /**
     * 获取所有课程，返回一个数据流。
     * 它可以用来监听数据的变化，以便 Widget 自动更新。
     */
    @Query("SELECT * FROM widget_courses ORDER BY date ASC, startTime ASC")
    fun getAllWidgetCourses(): Flow<List<WidgetCourse>>

    /**
     * 根据指定的日期范围获取课程。
     */
    @Query("SELECT * FROM widget_courses WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getWidgetCoursesByDateRange(startDate: String, endDate: String): Flow<List<WidgetCourse>>

    /**
     * 插入一个或多个课程。
     * 由于同步器会处理所有数据，我们使用 REPLACE 策略，在冲突时替换旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(courses: List<WidgetCourse>)

    /**
     * 删除所有课程。
     * 在同步前，我们可以清空旧数据。
     */
    @Query("DELETE FROM widget_courses")
    suspend fun deleteAll()
}