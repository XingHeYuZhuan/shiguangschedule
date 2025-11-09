package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 实体类，代表“应用设置”数据表。
 * 数据库中只会存在一条记录，使用固定 ID作为主键。
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1, // 固定主键，确保数据库中只有一条设置记录
    val currentCourseTableId: String? = null, // 当前正在使用的课表的 ID
    val reminderEnabled: Boolean = false, // 是否开启上课前提醒
    val remindBeforeMinutes: Int = 15, // 上课前提前多少分钟提醒，单位：分钟
    val skippedDates: Set<String>? = null, //跳过的日期
    val autoModeEnabled: Boolean = false, // 上课时行为模式的总开关状态
    val autoControlMode: String = "DND", // 控制上课时采取的具体模式 ("DND" 或 "SILENT")
)