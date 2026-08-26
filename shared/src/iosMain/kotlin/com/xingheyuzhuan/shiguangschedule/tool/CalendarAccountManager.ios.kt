package com.xingheyuzhuan.shiguangschedule.tool

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.getString
import platform.EventKit.EKAlarm
import platform.EventKit.EKAuthorizationStatusAuthorized
import platform.EventKit.EKAuthorizationStatusFullAccess
import platform.EventKit.EKAuthorizationStatusNotDetermined
import platform.EventKit.EKCalendar
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKit.EKSource
import platform.EventKit.EKSpan
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSPredicate
import platform.UIKit.UIDevice
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.course_teacher_prefix
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual object CalendarAccountManager {
    private const val CALENDAR_IDENTIFIER_KEY = "shiguangschedule.calendar.identifier"
    private const val APPLE_REFERENCE_DATE_EPOCH_SECONDS = 978_307_200.0

    private val eventStore = EKEventStore()

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun syncCurrentTableToSystemCalendar(
        courses: List<CourseWithWeeks>, timeSlots: List<TimeSlot>, semesterStartDate: LocalDate,
        semesterTotalWeeks: Int, firstDayOfWeekInt: Int, alarmMinutes: Int?, skippedDates: Set<String>?
    ): Boolean {
        return try {
            if (!ensureCalendarAccess()) return false

            val calendar = getOrCreateCalendar() ?: return false
            val timeZone = TimeZone.currentSystemDefault()
            if (!deleteExistingEvents(calendar, semesterStartDate, semesterTotalWeeks, timeZone)) return false

            if (semesterTotalWeeks <= 0 || courses.isEmpty()) return true

            IcsExportTool.processCourseInstances(
                courses = courses,
                timeSlots = timeSlots,
                semesterStartDate = semesterStartDate,
                semesterTotalWeeks = semesterTotalWeeks,
                firstDayOfWeekInt = firstDayOfWeekInt,
                skippedDates = skippedDates
            ) { course, start, end, _ ->
                val event = EKEvent.eventWithEventStore(eventStore).apply {
                    this.calendar = calendar
                    title = course.name
                    location = course.position.takeIf { it.isNotBlank() }
                    notes = course.teacher.takeIf { it.isNotBlank() }?.let { teacher ->
                        getString(Res.string.course_teacher_prefix, teacher)
                    }
                    startDate = dateFromEpochSeconds(start.toInstant(timeZone).epochSeconds.toDouble())
                    endDate = dateFromEpochSeconds(end.toInstant(timeZone).epochSeconds.toDouble())
                    if (alarmMinutes != null && alarmMinutes in 0..60) {
                        addAlarm(EKAlarm.alarmWithRelativeOffset(-(alarmMinutes * 60).toDouble()))
                    }
                }
                eventStore.saveEvent(event, span = EKSpan.EKSpanThisEvent, commit = false, error = null)
            }

            eventStore.commit(null)
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun ensureCalendarAccess(): Boolean {
        val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
        return when (status) {
            EKAuthorizationStatusAuthorized, EKAuthorizationStatusFullAccess -> true
            EKAuthorizationStatusNotDetermined -> requestCalendarAccess()
            else -> false
        }
    }

    private suspend fun requestCalendarAccess(): Boolean = suspendCoroutine { continuation ->
        val completion: (Boolean, NSError?) -> Unit = { granted, _ -> continuation.resume(granted) }
        if (UIDevice.currentDevice.systemVersion.substringBefore('.').toIntOrNull()?.let { it >= 17 } == true) {
            eventStore.requestFullAccessToEventsWithCompletion(completion)
        } else {
            eventStore.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent, completion)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun getOrCreateCalendar(): EKCalendar? {
        val defaults = platform.Foundation.NSUserDefaults.standardUserDefaults
        val storedIdentifier = defaults.stringForKey(CALENDAR_IDENTIFIER_KEY)
        val storedCalendar = storedIdentifier?.let { eventStore.calendarWithIdentifier(it) }
        if (storedCalendar != null) return storedCalendar

        val calendarTitle = getString(Res.string.app_name)
        val existingCalendar = eventStore.calendarsForEntityType(EKEntityType.EKEntityTypeEvent)
            .filterIsInstance<EKCalendar>()
            .firstOrNull { it.title == calendarTitle }
        if (existingCalendar != null) {
            defaults.setObject(existingCalendar.calendarIdentifier, forKey = CALENDAR_IDENTIFIER_KEY)
            return existingCalendar
        }

        val source = eventStore.defaultCalendarForNewEvents?.source
            ?: eventStore.sources.filterIsInstance<EKSource>().firstOrNull()
            ?: return null
        val calendar = EKCalendar.calendarForEntityType(EKEntityType.EKEntityTypeEvent, eventStore).apply {
            title = calendarTitle
            this.source = source
        }
        return if (eventStore.saveCalendar(calendar, commit = true, error = null)) {
            defaults.setObject(calendar.calendarIdentifier, forKey = CALENDAR_IDENTIFIER_KEY)
            calendar
        } else {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun deleteExistingEvents(
        calendar: EKCalendar,
        semesterStartDate: LocalDate,
        semesterTotalWeeks: Int,
        timeZone: TimeZone
    ): Boolean {
        val start = LocalDateTime(semesterStartDate, LocalTime(0, 0))
        val end = LocalDateTime(
            semesterStartDate.plus((semesterTotalWeeks.coerceAtLeast(1) * 7).toLong(), DateTimeUnit.DAY),
            LocalTime(23, 59)
        )
        val predicate: NSPredicate = eventStore.predicateForEventsWithStartDate(
            startDate = dateFromEpochSeconds(start.toInstant(timeZone).epochSeconds.toDouble()),
            endDate = dateFromEpochSeconds(end.toInstant(timeZone).epochSeconds.toDouble()),
            calendars = listOf(calendar)
        )
        eventStore.eventsMatchingPredicate(predicate).filterIsInstance<EKEvent>().forEach { event ->
            eventStore.removeEvent(event, span = EKSpan.EKSpanThisEvent, commit = false, error = null)
        }
        return eventStore.commit(null)
    }

    private fun dateFromEpochSeconds(epochSeconds: Double): NSDate {
        return NSDate(timeIntervalSinceReferenceDate = epochSeconds - APPLE_REFERENCE_DATE_EPOCH_SECONDS)
    }
}
