package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.di.appSettingsDataStore
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import com.xingheyuzhuan.shiguangschedule.data.repository.scheduleGridStyleDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WidgetSnapshotLoader {
    private const val WIDGET_SYNC_DAYS = 7L

    suspend fun load(
        context: Context,
        widgetCourseFontScaleOverride: Float? = null
    ): WidgetSnapshot {
        val mainDb = MainAppDatabase.getDatabase(context)
        val today = LocalDate.now()

        val currentStyle = withTimeoutOrNull(2000L) {
            context.scheduleGridStyleDataStore.data.first()
        }

        val finalStyleToSync = (currentStyle ?: ScheduleGridStyle.DEFAULT.toProto())
            .let { baseStyle ->
                if (baseStyle.course_color_maps.isEmpty()) {
                    baseStyle.copy(course_color_maps = ScheduleGridStyle.DEFAULT.toProto().course_color_maps)
                } else {
                    baseStyle
                }
            }
            .let { baseStyle ->
                val overrideScale = widgetCourseFontScaleOverride?.coerceIn(0.5f, 2.0f)
                if (overrideScale != null) {
                    baseStyle.copy(widget_course_font_scale = overrideScale)
                } else {
                    baseStyle
                }
            }

        val fallbackCourseTable = withTimeoutOrNull(2000L) {
            mainDb.courseTableDao().getFirstTableOnce()
        }
        val fallbackCourseTableId = fallbackCourseTable?.id.orEmpty()

        val currentAppSettings = withTimeoutOrNull(2000L) {
            context.appSettingsDataStore.data.first().let { prefs ->
                AppSettingsModel.fromPreferences(prefs, fallbackTableId = fallbackCourseTableId)
            }
        }

        val resolvedCourseTableId = currentAppSettings?.currentCourseTableId
            ?.takeIf { it.isNotBlank() }
            ?: fallbackCourseTableId

        val currentCourseTable = withTimeoutOrNull(2000L) {
            mainDb.courseTableDao().getCourseTableById(resolvedCourseTableId)
                ?: fallbackCourseTable
        } ?: fallbackCourseTable

        val currentCourseTableName = currentCourseTable?.name.orEmpty()
        val currentCourseConfig = if (resolvedCourseTableId.isBlank()) {
            null
        } else {
            withTimeoutOrNull(2000L) {
                mainDb.courseTableConfigDao().getConfigOnce(resolvedCourseTableId)
            }
        }
        val firstDayOfWeek = currentCourseConfig?.firstDayOfWeek?.takeIf { it in 1..7 } ?: 1
        val showWeekends = currentCourseConfig?.showWeekends ?: false

        val timeSlots = if (resolvedCourseTableId.isBlank()) {
            emptyList()
        } else {
            withTimeoutOrNull(2000L) {
                mainDb.timeSlotDao()
                    .getTimeSlotsByCourseTableId(resolvedCourseTableId)
                    .first()
                    .filter { it.number > 0 && it.startTime.isNotBlank() }
                    .sortedBy { it.number }
            } ?: emptyList()
        }
        val timeSlotProtoList = timeSlots.map { slot ->
            WidgetTimeSlotProto(
                number = slot.number,
                start_time = slot.startTime,
                end_time = slot.endTime
            )
        }

        val coursesWithWeeks = if (resolvedCourseTableId.isBlank()) {
            emptyList()
        } else {
            withTimeoutOrNull(3000L) {
                mainDb.courseDao().getCoursesWithWeeksByTableId(resolvedCourseTableId).first()
            } ?: emptyList()
        }

        val courseProtoList = buildCourseSnapshotCourses(
            today = today,
            appSettings = currentAppSettings,
            config = currentCourseConfig,
            coursesWithWeeks = coursesWithWeeks,
            timeSlots = timeSlots
        )

        return WidgetSnapshot(
            current_week = calculateCurrentWeek(
                today = today,
                config = currentCourseConfig,
                firstDayOfWeek = firstDayOfWeek
            ) ?: 0,
            style = finalStyleToSync,
            courses = courseProtoList,
            time_slots = timeSlotProtoList,
            course_table_name = currentCourseTableName,
            show_weekends = showWeekends,
            first_day_of_week = firstDayOfWeek
        )
    }

    private fun buildCourseSnapshotCourses(
        today: LocalDate,
        appSettings: AppSettingsModel?,
        config: CourseTableConfig?,
        coursesWithWeeks: List<CourseWithWeeks>,
        timeSlots: List<TimeSlot>
    ): List<WidgetCourseProto> {
        val semesterStartDateText = config?.semesterStartDate ?: return emptyList()
        val semesterTotalWeeks = config.semesterTotalWeeks
        if (semesterTotalWeeks <= 0) return emptyList()

        val firstDayOfWeek = config.firstDayOfWeek.takeIf { it in 1..7 } ?: 1
        val semesterStartDate = runCatching { LocalDate.parse(semesterStartDateText) }.getOrNull()
            ?: return emptyList()
        val alignedSemesterStartDate = startOfConfiguredWeek(semesterStartDate, firstDayOfWeek)
        val displayWeekStartDate = startOfConfiguredWeek(today, firstDayOfWeek)
        val displayWeekEndDate = displayWeekStartDate.plusDays(6)
        val futureSyncEndDate = today.plusDays(WIDGET_SYNC_DAYS - 1)
        val startSyncDate = if (today.isBefore(alignedSemesterStartDate)) {
            alignedSemesterStartDate
        } else {
            minOf(displayWeekStartDate, today)
        }
        val endSyncDate = if (today.isBefore(alignedSemesterStartDate)) {
            alignedSemesterStartDate.plusDays(WIDGET_SYNC_DAYS - 1)
        } else {
            maxOf(displayWeekEndDate, futureSyncEndDate)
        }

        val timeSlotMap = timeSlots.associateBy { it.number }
        val skippedDates = appSettings?.skippedDates.orEmpty()
        val result = mutableListOf<WidgetCourseProto>()
        val syncDays = ChronoUnit.DAYS.between(startSyncDate, endSyncDate).toInt() + 1

        for (offset in 0 until syncDays) {
            val date = startSyncDate.plusDays(offset.toLong())
            val alignedDate = startOfConfiguredWeek(date, firstDayOfWeek)
            val weekNumber = ChronoUnit.WEEKS.between(alignedSemesterStartDate, alignedDate).toInt() + 1
            if (weekNumber !in 1..semesterTotalWeeks) continue

            val dayOfWeek = date.dayOfWeek.value
            val dateText = date.toString()

            coursesWithWeeks.forEach { courseWithWeeks ->
                if (courseWithWeeks.course.day != dayOfWeek) return@forEach
                if (courseWithWeeks.weeks.none { it.weekNumber == weekNumber }) return@forEach

                val course = courseWithWeeks.course
                val (startTime, endTime) = if (course.isCustomTime) {
                    (course.customStartTime ?: "") to (course.customEndTime ?: "")
                } else {
                    (timeSlotMap[course.startSection]?.startTime ?: "") to
                        (timeSlotMap[course.endSection]?.endTime ?: "")
                }

                result += WidgetCourseProto(
                    id = "${course.id}-$dateText",
                    name = course.name,
                    teacher = course.teacher,
                    position = course.position,
                    start_time = startTime,
                    end_time = endTime,
                    color_int = course.colorInt,
                    is_skipped = skippedDates.contains(dateText),
                    date = dateText
                )
            }
        }

        return result.sortedWith(compareBy({ it.date }, { it.start_time }, { it.name }))
    }

    private fun calculateCurrentWeek(
        today: LocalDate,
        config: CourseTableConfig?,
        firstDayOfWeek: Int
    ): Int? {
        val semesterStartDateText = config?.semesterStartDate ?: return null
        val semesterTotalWeeks = config.semesterTotalWeeks
        if (semesterTotalWeeks <= 0) return null

        val semesterStartDate = runCatching { LocalDate.parse(semesterStartDateText) }.getOrNull()
            ?: return null
        val alignedSemesterStartDate = startOfConfiguredWeek(semesterStartDate, firstDayOfWeek)
        val alignedToday = startOfConfiguredWeek(today, firstDayOfWeek)
        if (alignedToday.isBefore(alignedSemesterStartDate)) return null

        val weekNumber = ChronoUnit.WEEKS.between(alignedSemesterStartDate, alignedToday).toInt() + 1
        return weekNumber.takeIf { it in 1..semesterTotalWeeks }
    }
}
