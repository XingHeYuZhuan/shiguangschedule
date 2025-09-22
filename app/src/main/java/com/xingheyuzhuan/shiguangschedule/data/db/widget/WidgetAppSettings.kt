package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_semester_start_date")
data class WidgetAppSettings(
    @PrimaryKey
    val id: Int = 1,
    val semesterStartDate: String? = null,  //第一周的时间
    val semesterTotalWeeks: Int = 20 // 最大周数
)