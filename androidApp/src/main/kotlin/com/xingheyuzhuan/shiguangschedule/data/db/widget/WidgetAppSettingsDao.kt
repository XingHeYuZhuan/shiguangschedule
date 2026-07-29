// 文件名：WidgetAppSettingsDao.kt
package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetAppSettingsDao {
    /**
     * 获取小组件设置，返回一个可以为空的数据流。
     * 当表中没有数据时，它会发出一个 null 值。
     */
    @Query("SELECT * FROM widget_semester_start_date WHERE id = 1")
    fun getAppSettings(): Flow<WidgetAppSettings?>

    /**
     * 插入或更新小组件设置。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: WidgetAppSettings)
}