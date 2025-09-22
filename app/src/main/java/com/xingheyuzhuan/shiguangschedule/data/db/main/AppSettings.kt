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

    val showWeekends: Boolean = false, // 是否在课表中显示周末（周六、周日）
    val currentCourseTableId: String? = null, // 当前正在使用的课表的 ID
    val semesterStartDate: String? = null, // 学期开始日期，用于计算当前周数
    val semesterTotalWeeks: Int = 20, // 本学期的总周数
    val defaultClassDuration: Int = 45, // 默认的上课时长，单位：分钟
    val defaultBreakDuration: Int = 10, // 默认的下课休息时长，单位：分钟

    val reminderEnabled: Boolean = false, // 是否开启上课前提醒
    val remindBeforeMinutes: Int = 15, // 上课前提前多少分钟提醒，单位：分钟
    val skippedDates: Set<String>? = null //跳过的日期

)