package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "widget_semester_start_date")
data class WidgetAppSettings(
    @PrimaryKey
    val id: Int = 1,
    val semesterStartDate: String? = null,  //第一周的时间
    val semesterTotalWeeks: Int = 20, // 最大周数
    val firstDayOfWeek: Int = DayOfWeek.MONDAY.value // 一周的起始日，1=周一，7=周日
)