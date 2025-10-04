package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room 实体类，代表“时间段”数据表。
 * 存储每节课的节次编号和对应的开始/结束时间。
 */
@Entity(
    tableName = "time_slots",
    // 外键约束，关联 CourseTable
    foreignKeys = [
        ForeignKey(
            entity = CourseTable::class,
            parentColumns = ["id"],
            childColumns = ["courseTableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseTableId"])],
    // 联合主键，以确保同一个课表中节次编号是唯一的
    primaryKeys = ["number", "courseTableId"]
)
data class TimeSlot(
    val number: Int, // 节次编号作为主键的一部分
    val startTime: String, // 开始时间，例如 "08:00"
    val endTime: String, // 结束时间，例如 "08:45"
    val courseTableId: String //对应的课表id
)