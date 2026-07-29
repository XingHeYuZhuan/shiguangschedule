// File: IcsExportTool.kt

package com.xingheyuzhuan.shiguangschedule.tool

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.CalendarContract
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * 课表数据转换工具类
 * * 核心功能：
 * 1. 计算一学期中所有课程的精确发生日期。
 * 2. 将课表导出为标准 ICS 日历文件格式（供外部日历软件导入）。
 * 3. 生成 Android 系统日历批量操作指令（用于应用内一键同步）。
 */
object IcsExportTool {

    /** ICS 标准 UTC 时间格式化器 */
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    /** 课表内部时间格式化器 */
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * 【内部核心引擎】通用课程实例处理器
     * * 该方法封装了最复杂的课表日期推算算法：
     * - 根据学期起始日对齐首周。
     * - 处理课程的多周重复逻辑。
     * - 自动解析“标准节次”与“自定义时间”两种模式。
     * - 校验周数有效性并排除跳过日期。
     *
     * @param courses 原始课程及其关联周数数据。
     * @param timeSlots 时间槽定义（用于非自定义时间模式）。
     * @param semesterStartDate 学期第一周的起始日期。
     * @param semesterTotalWeeks 学期总周数，用于边界截断。
     * @param firstDayOfWeekInt 一周的起始日（1=周一, 7=周日）。
     * @param skippedDates 排除日期集合（格式: yyyy-MM-dd）。
     * @param action 回调函数，为每个计算出的课程实例提供具体的时间和元数据。
     */
    private inline fun processCourseInstances(
        courses: List<CourseWithWeeks>,
        timeSlots: List<TimeSlot>,
        semesterStartDate: LocalDate,
        semesterTotalWeeks: Int,
        firstDayOfWeekInt: Int,
        skippedDates: Set<String>? = null,
        action: (course: Course, startDateTime: LocalDateTime, endDateTime: LocalDateTime, weekNumber: Int) -> Unit
    ) {
        val timeSlotMap = timeSlots.associateBy { it.number }
        val dayOfWeekMap = mapOf(
            1 to DayOfWeek.MONDAY, 2 to DayOfWeek.TUESDAY, 3 to DayOfWeek.WEDNESDAY,
            4 to DayOfWeek.THURSDAY, 5 to DayOfWeek.FRIDAY, 6 to DayOfWeek.SATURDAY, 7 to DayOfWeek.SUNDAY
        )

        // 确定周首基准：对齐到用户设置的一周起始日
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)
        val alignedSemesterStart = semesterStartDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

        courses.forEach { courseWithWeeks ->
            val course = courseWithWeeks.course
            val weeks = courseWithWeeks.weeks.map { it.weekNumber }

            // 解析时间：区分自定义 HH:mm 字符串和标准节次
            val startTime: LocalTime
            val endTime: LocalTime
            if (course.isCustomTime) {
                val s = course.customStartTime ?: return@forEach
                val e = course.customEndTime ?: return@forEach
                try {
                    startTime = LocalTime.parse(s, TIME_FORMATTER)
                    endTime = LocalTime.parse(e, TIME_FORMATTER)
                } catch (ex: Exception) { return@forEach }
            } else {
                val s = timeSlotMap[course.startSection]?.startTime ?: return@forEach
                val e = timeSlotMap[course.endSection]?.endTime ?: return@forEach
                try {
                    startTime = LocalTime.parse(s, TIME_FORMATTER)
                    endTime = LocalTime.parse(e, TIME_FORMATTER)
                } catch (ex: Exception) { return@forEach }
            }

            val dayOfWeek = dayOfWeekMap[course.day] ?: return@forEach

            // 迭代每一周，计算具体日期
            weeks.forEach { week ->
                val dayOffset = (dayOfWeek.value - firstDayOfWeek.value + 7) % 7
                val date = alignedSemesterStart.plusWeeks((week - 1).toLong()).plusDays(dayOffset.toLong())

                val weekIndex = ChronoUnit.DAYS.between(alignedSemesterStart, date) / 7 + 1
                if (weekIndex > semesterTotalWeeks) return@forEach

                if (skippedDates?.contains(date.toString()) == true) return@forEach

                action(course, LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime), week)
            }
        }
    }

    /**
     * 将课表转换为标准 ICS 字符串
     */
    fun generateIcsFileContent(
        context: Context,
        courses: List<CourseWithWeeks>,
        timeSlots: List<TimeSlot>,
        semesterStartDate: LocalDate,
        semesterTotalWeeks: Int,
        firstDayOfWeekInt: Int,
        alarmMinutes: Int? = null,
        skippedDates: Set<String>? = null
    ): String {
        val ics = StringBuilder()

        ics.append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//ShiGuangSchedule//ZH\r\n")
        ics.append("BEGIN:VTIMEZONE\r\nTZID:Asia/Shanghai\r\nBEGIN:STANDARD\r\n")
        ics.append("DTSTART:19700101T000000\r\nTZOFFSETFROM:+0800\r\nTZOFFSETTO:+0800\r\n")
        ics.append("END:STANDARD\r\nEND:VTIMEZONE\r\n")

        val teacherPrefix = context.getString(R.string.course_teacher_prefix)

        processCourseInstances(
            courses, timeSlots, semesterStartDate, semesterTotalWeeks, firstDayOfWeekInt, skippedDates
        ) { course, start, end, _ ->
            ics.append("BEGIN:VEVENT\r\n")
            ics.append("UID:${UUID.randomUUID()}@shiguangschedule.com\r\n")
            ics.append("DTSTAMP:${formatDateTimeUtc(LocalDateTime.now())}\r\n")
            ics.append("DTSTART;TZID=Asia/Shanghai:${formatDateTimeLocal(start)}\r\n")
            ics.append("DTEND;TZID=Asia/Shanghai:${formatDateTimeLocal(end)}\r\n")
            ics.append("SUMMARY:${escapeText(course.name)}\r\n")
            ics.append("LOCATION:${escapeText(course.position)}\r\n")

            val teacherDescription = String.format(teacherPrefix, course.teacher)
            ics.append("DESCRIPTION:${escapeText(teacherDescription)}\r\n")

            // 处理通知策略
            if (alarmMinutes != null && alarmMinutes in 0..60) {
                ics.append("BEGIN:VALARM\r\nACTION:DISPLAY\r\nDESCRIPTION:课程提醒\r\n")
                ics.append("TRIGGER:-PT${alarmMinutes}M\r\nEND:VALARM\r\n")
            }
            ics.append("END:VEVENT\r\n")
        }

        ics.append("END:VCALENDAR\r\n")
        return ics.toString()
    }

    /**
     * 生成同步到 Android 系统日历的批量指令
     */
    fun generateCalendarOps(
        context: Context,
        courses: List<CourseWithWeeks>,
        timeSlots: List<TimeSlot>,
        semesterStartDate: LocalDate,
        semesterTotalWeeks: Int,
        firstDayOfWeekInt: Int,
        calendarId: Long,
        alarmMinutes: Int? = null,
        skippedDates: Set<String>? = null
    ): ArrayList<ContentProviderOperation> {
        val ops = ArrayList<ContentProviderOperation>()

        val teacherPrefix = context.getString(R.string.course_teacher_prefix)

        processCourseInstances(
            courses, timeSlots, semesterStartDate, semesterTotalWeeks, firstDayOfWeekInt, skippedDates
        ) { course, start, end, _ ->

            val startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val eventOpIndex = ops.size

            val teacherDescription = String.format(teacherPrefix, course.teacher)

            ops.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                .withValue(CalendarContract.Events.TITLE, course.name)
                .withValue(CalendarContract.Events.EVENT_LOCATION, course.position)
                .withValue(CalendarContract.Events.DESCRIPTION, teacherDescription)
                .withValue(CalendarContract.Events.DTSTART, startMillis)
                .withValue(CalendarContract.Events.DTEND, endMillis)
                .withValue(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
                .withValue(CalendarContract.Events.HAS_ALARM, if (alarmMinutes != null) 1 else 0)
                .build())

            if (alarmMinutes != null && alarmMinutes >= 0) {
                ops.add(ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
                    .withValueBackReference(CalendarContract.Reminders.EVENT_ID, eventOpIndex)
                    .withValue(CalendarContract.Reminders.MINUTES, alarmMinutes)
                    .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    .build())
            }
        }
        return ops
    }

    /** 格式化本地时间为 ICS 字符串 (无 Z) */
    private fun formatDateTimeLocal(dateTime: LocalDateTime): String {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
    }

    /** 格式化 UTC 时间并添加 Z 标记 */
    private fun formatDateTimeUtc(dateTime: LocalDateTime): String {
        val utc = dateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        return utc.format(DATE_TIME_FORMATTER)
    }

    /** 转义 ICS 内容中的特殊敏感字符 */
    private fun escapeText(text: String): String {
        return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
    }
}