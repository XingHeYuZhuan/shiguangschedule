package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 实体类，专为桌面 Widget 设计的课程数据表。
 * 此表只包含 Widget 显示所需的最精简信息，数据由主数据库同步器预处理。
 */
@Entity(tableName = "widget_courses")
data class WidgetCourse(
    @PrimaryKey
    val id: String, // 课程的唯一标识符
    val name: String, // 课程名称
    val teacher: String, // 老师姓名
    val position: String, // 上课地点
    val startTime: String, // 课程开始时间，例如 "08:00"
    val endTime: String, // 课程结束时间，例如 "08:45"
    val isSkipped: Boolean = false, // 是否跳过该课程
    val date: String, // 课程的真实上课日期，例如 "2025-09-15"
    val colorInt: Int = 0 // 课程颜色，存储为Int类型
)