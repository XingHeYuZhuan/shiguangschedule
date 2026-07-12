package com.xingheyuzhuan.shiguangschedule.widget

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun WidgetSnapshot.firstDayOfWeekValue(): Int {
    return first_day_of_week.takeIf { it in DayOfWeek.MONDAY.value..DayOfWeek.SUNDAY.value }
        ?: DayOfWeek.MONDAY.value
}

fun startOfConfiguredWeek(date: LocalDate, firstDayOfWeek: Int): LocalDate {
    val normalizedDay = firstDayOfWeek.takeIf { it in DayOfWeek.MONDAY.value..DayOfWeek.SUNDAY.value }
        ?: DayOfWeek.MONDAY.value
    return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(normalizedDay)))
}
