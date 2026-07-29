package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 一个用于关联查询的数据类。
 * 它将一个 Course 实体和一个 CourseWeek 列表组合在一起。
 *
 * 这解决了 Room 中一个课程对应多个周数的复杂查询问题。
 */
data class CourseWithWeeks(
    @Embedded
    val course: Course,
    @Relation(
        parentColumn = "id", // Course 的主键
        entityColumn = "courseId" // CourseWeek 中引用 Course 的外键
    )
    val weeks: List<CourseWeek>
)