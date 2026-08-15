package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

@Entity(tableName = "widget_semester_start_date")
data class WidgetAppSettings(
    @PrimaryKey
    val id: Int = 1,
    val semesterStartDate: String? = null,  //第一周的时间
    val semesterTotalWeeks: Int = 20, // 最大周数
    val firstDayOfWeek: Int = DayOfWeek.MONDAY.isoDayNumber // 一周的起始日，1=周一，7=周日
)