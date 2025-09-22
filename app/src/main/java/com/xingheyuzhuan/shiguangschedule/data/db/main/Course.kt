package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room 实体类，代表“课程”数据表。
 * 它存储每门课程的详细信息，并与 `CourseTable` 关联。
 */
@Entity(
    tableName = "courses",
    foreignKeys = [ForeignKey(
        entity = CourseTable::class,
        parentColumns = ["id"],
        childColumns = ["courseTableId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["courseTableId"])]
)
data class Course(
    @PrimaryKey
    val id: String, // 课程的唯一标识符 UUID
    val courseTableId: String, // 课程所属的课表 ID
    val name: String, // 课程名称，例如 "大学物理"
    val teacher: String, // 授课老师姓名
    val position: String, // 上课地点，例如 "主楼A101"
    val day: Int, // 上课的星期几，例如 1=周一, 7=周日
    val startSection: Int, // 课程开始的节次
    val endSection: Int,   // 课程结束的节次
    val colorInt: Int // 课程卡片的颜色值
)