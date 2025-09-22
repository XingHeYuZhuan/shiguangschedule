package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlotDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TimeSlotRepository(
    private val timeSlotDao: TimeSlotDao
) {
    /**
     * 获取指定课表的所有时间段，返回一个数据流。
     */
    fun getTimeSlotsByCourseTableId(courseTableId: String): Flow<List<TimeSlot>> {
        return timeSlotDao.getTimeSlotsByCourseTableId(courseTableId)
    }

    /**
     * 插入或更新所有时间段。
     */
    suspend fun insertAll(timeSlots: List<TimeSlot>) {
        timeSlotDao.insertAll(timeSlots)
    }

    /**
     * 完全替换指定课表下的所有时间段数据。
     */
    suspend fun replaceAllForCourseTable(courseTableId: String, timeSlots: List<TimeSlot>) = withContext(Dispatchers.IO) {
        // 先删除旧数据，再插入新数据
        timeSlotDao.deleteAllTimeSlotsByCourseTableId(courseTableId)
        if (timeSlots.isNotEmpty()) {
            timeSlotDao.insertAll(timeSlots)
        }
    }
}