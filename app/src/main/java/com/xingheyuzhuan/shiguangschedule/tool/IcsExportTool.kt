// File: IcsExportTool.kt

package com.xingheyuzhuan.shiguangschedule.tool

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

object IcsExportTool {

    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    /**
     * 【新增】生成 ICS 日历文件的内容字符串。
     *
     * @param courses 包含所有课程和周数的数据列表。
     * @param timeSlots 包含所有时间段的数据列表。
     * @param semesterStartDate 学期开始日期。
     * @param semesterTotalWeeks 本学期的总周数。
     * @param alarmMinutes 可选的提醒时间，单位分钟。传入null则不设置提醒。
     * @param skippedDates 包含需要跳过的日期的字符串集合，格式为 yyyy-MM-dd。
     * @return ICS 格式的字符串。
     */
    fun generateIcsFileContent(
        courses: List<CourseWithWeeks>,
        timeSlots: List<TimeSlot>,
        semesterStartDate: LocalDate,
        semesterTotalWeeks: Int,
        alarmMinutes: Int? = null,
        skippedDates: Set<String>? = null
    ): String {
        val icsContent = StringBuilder()

        // 1. 拼接 ICS 文件的头部
        icsContent.append("BEGIN:VCALENDAR\r\n")
        icsContent.append("VERSION:2.0\r\n")
        icsContent.append("PRODID:-//ShiGuangSchedule//ZH\r\n")

        // 添加 Asia/Shanghai 的时区定义
        icsContent.append("BEGIN:VTIMEZONE\r\n")
        icsContent.append("TZID:Asia/Shanghai\r\n")
        icsContent.append("BEGIN:STANDARD\r\n")
        icsContent.append("DTSTART:19700101T000000\r\n")
        icsContent.append("TZOFFSETFROM:+0800\r\n")
        icsContent.append("TZOFFSETTO:+0800\r\n")
        icsContent.append("END:STANDARD\r\n")
        icsContent.append("END:VTIMEZONE\r\n")

        val timeSlotMap = timeSlots.associateBy { it.number }
        val dayOfWeekMap = mapOf(
            1 to DayOfWeek.MONDAY,
            2 to DayOfWeek.TUESDAY,
            3 to DayOfWeek.WEDNESDAY,
            4 to DayOfWeek.THURSDAY,
            5 to DayOfWeek.FRIDAY,
            6 to DayOfWeek.SATURDAY,
            7 to DayOfWeek.SUNDAY
        )

        // 2. 为每个课程生成一个或多个 VEVENT
        courses.forEach { courseWithWeeks ->
            val course = courseWithWeeks.course
            val weeks = courseWithWeeks.weeks.map { it.weekNumber }

            val startSectionTime = timeSlotMap[course.startSection]?.startTime?.let {
                LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
            } ?: return@forEach

            val endSectionTime = timeSlotMap[course.endSection]?.endTime?.let {
                LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
            } ?: return@forEach

            val dayOfWeek = dayOfWeekMap[course.day] ?: return@forEach

            weeks.forEach { week ->
                // 使用 semesterStartDate 计算课程的日期
                val date = semesterStartDate.plusWeeks((week - 1).toLong())
                    .plusDays(dayOfWeek.value.toLong() - 1)

                // 检查周数是否在有效范围内
                val weekNumberFromStart = ChronoUnit.DAYS.between(semesterStartDate, date) / 7 + 1
                if (weekNumberFromStart > semesterTotalWeeks) {
                    return@forEach // 跳过超出学期总周数的课程
                }

                // 检查该日期是否在跳过列表中
                if (skippedDates?.contains(date.toString()) == true) {
                    return@forEach
                }

                // 组合日期和时间
                val startDateTime = LocalDateTime.of(date, startSectionTime)
                val endDateTime = LocalDateTime.of(date, endSectionTime)

                // 拼接 VEVENT 事件块
                icsContent.append("BEGIN:VEVENT\r\n")
                icsContent.append("UID:${UUID.randomUUID()}@shiguangschedule.com\r\n")
                icsContent.append("DTSTAMP:${formatDateTimeUtc(LocalDateTime.now())}\r\n")
                icsContent.append("DTSTART;TZID=Asia/Shanghai:${formatDateTimeLocal(startDateTime)}\r\n")
                icsContent.append("DTEND;TZID=Asia/Shanghai:${formatDateTimeLocal(endDateTime)}\r\n")
                icsContent.append("SUMMARY:${escapeText(course.name)}\r\n")
                icsContent.append("LOCATION:${escapeText(course.position)}\r\n")
                val description = "教师: ${course.teacher}"
                icsContent.append("DESCRIPTION:${escapeText(description)}\r\n")

                // 根据传入参数决定是否添加VALARM提醒
                if (alarmMinutes != null && alarmMinutes in 0..60) {
                    icsContent.append("BEGIN:VALARM\r\n")
                    icsContent.append("ACTION:DISPLAY\r\n")
                    icsContent.append("DESCRIPTION:课程提醒\r\n")
                    icsContent.append("TRIGGER:-PT${alarmMinutes}M\r\n")
                    icsContent.append("END:VALARM\r\n")
                }

                icsContent.append("END:VEVENT\r\n")
            }
        }

        // 4. 拼接 ICS 文件的尾部
        icsContent.append("END:VCALENDAR\r\n")

        return icsContent.toString()
    }

    /**
     * 将 LocalDateTime 格式化为 ICS 标准的本地时间字符串。
     * 格式: YYYYMMDDTHHMMSS
     */
    private fun formatDateTimeLocal(dateTime: LocalDateTime): String {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
    }

    /**
     * 将 LocalDateTime 格式化为 ICS 标准的 UTC 时间字符串。
     * 格式: YYYYMMDDTHHMMSSZ
     */
    private fun formatDateTimeUtc(dateTime: LocalDateTime): String {
        val utcDateTime = dateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        return utcDateTime.format(DATE_TIME_FORMATTER)
    }

    /**
     * 对 ICS 文本内容中的特殊字符进行转义。
     */
    private fun escapeText(text: String): String {
        return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
    }
}