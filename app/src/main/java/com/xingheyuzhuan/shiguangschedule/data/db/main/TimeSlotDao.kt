package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作时间段 (TimeSlot) 数据表。
 */
@Dao
interface TimeSlotDao {
    /**
     * 获取指定课表的所有时间段，并按节次编号升序排列。
     */
    @Query("SELECT * FROM time_slots WHERE courseTableId = :courseTableId ORDER BY number ASC")
    fun getTimeSlotsByCourseTableId(courseTableId: String): Flow<List<TimeSlot>>

    /**
     * 根据节次编号和课表ID获取单个时间段。
     */
    @Query("SELECT * FROM time_slots WHERE number = :number AND courseTableId = :courseTableId LIMIT 1")
    suspend fun getTimeSlot(number: Int, courseTableId: String): TimeSlot?

    /**
     * 插入一个或多个时间段。如果发生主键冲突，则替换旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(timeSlots: List<TimeSlot>)

    /**
     * 更新一个现有时间段。
     */
    @Update
    suspend fun update(timeSlot: TimeSlot)

    /**
     * 删除一个或多个时间段。
     */
    @Delete
    suspend fun delete(timeSlot: TimeSlot)

    /**
     * 根据课表ID删除所有时间段。
     */
    @Query("DELETE FROM time_slots WHERE courseTableId = :courseTableId")
    suspend fun deleteAllTimeSlotsByCourseTableId(courseTableId: String)
}