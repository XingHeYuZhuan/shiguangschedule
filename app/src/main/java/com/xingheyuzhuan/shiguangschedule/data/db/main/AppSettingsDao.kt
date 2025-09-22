package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作应用设置 (AppSettings) 数据表。
 * 此表仅包含一条记录。
 */
@Dao
interface AppSettingsDao {
    /**
     * 获取应用设置。
     * 返回一个 Flow，当设置变化时会自动更新。
     * 注意：返回值类型改为可空类型 Flow<AppSettings?>。
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getAppSettings(): Flow<AppSettings?>

    /**
     * 插入或更新应用设置。
     * 由于表只会有一条记录，我们使用 REPLACE 策略来确保每次都是更新操作。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(appSettings: AppSettings)
}