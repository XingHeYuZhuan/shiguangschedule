package com.xingheyuzhuan.shiguangschedule.widget.week_timeline

import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.firstDayOfWeekValue
import com.xingheyuzhuan.shiguangschedule.widget.startOfConfiguredWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object WeekTimelineHeaderBinder {
    private val fullDayContainerIds = intArrayOf(
        R.id.layout_week_timeline_day_0,
        R.id.layout_week_timeline_day_1,
        R.id.layout_week_timeline_day_2,
        R.id.layout_week_timeline_day_3,
        R.id.layout_week_timeline_day_4,
        R.id.layout_week_timeline_day_5,
        R.id.layout_week_timeline_day_6
    )

    private val fullDayLabelIds = intArrayOf(
        R.id.tv_week_timeline_day_label_0,
        R.id.tv_week_timeline_day_label_1,
        R.id.tv_week_timeline_day_label_2,
        R.id.tv_week_timeline_day_label_3,
        R.id.tv_week_timeline_day_label_4,
        R.id.tv_week_timeline_day_label_5,
        R.id.tv_week_timeline_day_label_6
    )

    private val fullDayDateIds = intArrayOf(
        R.id.tv_week_timeline_day_date_0,
        R.id.tv_week_timeline_day_date_1,
        R.id.tv_week_timeline_day_date_2,
        R.id.tv_week_timeline_day_date_3,
        R.id.tv_week_timeline_day_date_4,
        R.id.tv_week_timeline_day_date_5,
        R.id.tv_week_timeline_day_date_6
    )

    private val weekdayDayContainerIds = intArrayOf(
        R.id.layout_week_timeline_weekday_day_0,
        R.id.layout_week_timeline_weekday_day_1,
        R.id.layout_week_timeline_weekday_day_2,
        R.id.layout_week_timeline_weekday_day_3,
        R.id.layout_week_timeline_weekday_day_4
    )

    private val weekdayDayLabelIds = intArrayOf(
        R.id.tv_week_timeline_weekday_day_label_0,
        R.id.tv_week_timeline_weekday_day_label_1,
        R.id.tv_week_timeline_weekday_day_label_2,
        R.id.tv_week_timeline_weekday_day_label_3,
        R.id.tv_week_timeline_weekday_day_label_4
    )

    private val weekdayDayDateIds = intArrayOf(
        R.id.tv_week_timeline_weekday_day_date_0,
        R.id.tv_week_timeline_weekday_day_date_1,
        R.id.tv_week_timeline_weekday_day_date_2,
        R.id.tv_week_timeline_weekday_day_date_3,
        R.id.tv_week_timeline_weekday_day_date_4
    )

    private val weekDaysFull = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    private val weekDaysShort = listOf("一", "二", "三", "四", "五", "六", "日")

    fun bind(remoteViews: RemoteViews, snapshot: WidgetSnapshot) {
        val today = LocalDate.now()
        val weekStart = startOfConfiguredWeek(today, snapshot.firstDayOfWeekValue())
        val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }
        val primaryText = Color.BLACK
        val secondaryText = ColorUtils.setAlphaComponent(Color.BLACK, 190)
        val hintText = ColorUtils.setAlphaComponent(Color.BLACK, 148)

        remoteViews.setTextViewText(
            R.id.tv_week_timeline_date,
            today.format(DateTimeFormatter.ofPattern("yyyy/M/d", Locale.getDefault()))
        )
        remoteViews.setTextViewText(R.id.tv_week_timeline_subtitle, buildSubtitle(snapshot, today))
        remoteViews.setTextColor(R.id.tv_week_timeline_date, primaryText)
        remoteViews.setTextColor(R.id.tv_week_timeline_subtitle, secondaryText)
        remoteViews.setTextViewText(R.id.tv_week_timeline_month_number, today.monthValue.toString())
        remoteViews.setTextColor(R.id.tv_week_timeline_month_number, primaryText)
        remoteViews.setTextColor(R.id.tv_week_timeline_month_label, hintText)

        if (snapshot.show_weekends) {
            remoteViews.setViewVisibility(R.id.layout_week_timeline_days_container_full, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.layout_week_timeline_days_container_weekday, View.GONE)
            bindDayRow(
                remoteViews = remoteViews,
                dates = weekDates,
                containerIds = fullDayContainerIds,
                labelIds = fullDayLabelIds,
                dateIds = fullDayDateIds,
                today = today,
                primaryText = primaryText,
                secondaryText = secondaryText,
                hintText = hintText
            )
        } else {
            remoteViews.setViewVisibility(R.id.layout_week_timeline_days_container_full, View.GONE)
            remoteViews.setViewVisibility(R.id.layout_week_timeline_days_container_weekday, View.VISIBLE)
            bindDayRow(
                remoteViews = remoteViews,
                dates = weekDates.take(5),
                containerIds = weekdayDayContainerIds,
                labelIds = weekdayDayLabelIds,
                dateIds = weekdayDayDateIds,
                today = today,
                primaryText = primaryText,
                secondaryText = secondaryText,
                hintText = hintText
            )
        }
    }

    private fun bindDayRow(
        remoteViews: RemoteViews,
        dates: List<LocalDate>,
        containerIds: IntArray,
        labelIds: IntArray,
        dateIds: IntArray,
        today: LocalDate,
        primaryText: Int,
        secondaryText: Int,
        hintText: Int
    ) {
        dates.forEachIndexed { index, date ->
            val isSelected = date == today
            remoteViews.setViewVisibility(containerIds[index], View.VISIBLE)
            remoteViews.setTextViewText(labelIds[index], weekDaysShort[date.dayOfWeek.value - 1])
            remoteViews.setTextViewText(dateIds[index], "${date.monthValue}/${date.dayOfMonth}")
            remoteViews.setTextColor(labelIds[index], if (isSelected) primaryText else secondaryText)
            remoteViews.setTextColor(dateIds[index], if (isSelected) primaryText else hintText)
            remoteViews.setInt(
                containerIds[index],
                "setBackgroundResource",
                if (isSelected) R.drawable.widget_week_timeline_day_selected else android.R.color.transparent
            )
        }
    }

    private fun buildSubtitle(snapshot: WidgetSnapshot, today: LocalDate): String {
        val courseTableName = snapshot.course_table_name.ifBlank { "课表" }
        val weekLabel = snapshot.current_week
            .takeIf { it > 0 }
            ?.let { "第${it}周" }
            ?: "当前周"
        return "$courseTableName | $weekLabel ${weekDaysFull[today.dayOfWeek.value - 1]}"
    }
}
