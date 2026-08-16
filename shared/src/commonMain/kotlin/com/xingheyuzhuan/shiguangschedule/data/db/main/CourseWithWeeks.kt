package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Embedded
import androidx.room3.Relation

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
        parentColumns = ["id"],
        entityColumns = ["courseId"]
    )
    val weeks: List<CourseWeek>
)