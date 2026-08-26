package com.xingheyuzhuan.shiguangschedule.service

import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Single
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.label_position
import shiguangschedule.shared.generated.resources.label_teacher
import shiguangschedule.shared.generated.resources.notification_title_course_alert
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Single(createdAtStart = true)
class IosCourseNotificationScheduler(
    private val appSettingsRepository: AppSettingsRepository,
    private val widgetRepository: WidgetRepository
) {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    suspend fun rebuildNotifications() {
        cancelCourseNotifications()

        val settings = appSettingsRepository.getAppSettingsOnce()
        if (!settings.reminderEnabled) return
        if (!requestNotificationPermission()) return

        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date
        val courses = widgetRepository.getWidgetCoursesByDateRange(
            startDate = today.toString(),
            endDate = today.plus(WIDGET_SYNC_DAYS, DateTimeUnit.DAY).toString()
        ).first().filter { !it.isSkipped }

        val positionLabel = getString(Res.string.label_position)
        val teacherLabel = getString(Res.string.label_teacher)
        val alertTitle = getString(Res.string.notification_title_course_alert)

        courses.take(ALARM_SLOT_COUNT).forEachIndexed { index, course ->
            val startTime = runCatching {
                LocalDateTime(LocalDate.parse(course.date), LocalTime.parse(course.startTime))
                    .toInstant(timeZone)
            }.getOrNull() ?: return@forEachIndexed
            val remindTime = startTime - settings.remindBeforeMinutes.minutes
            if (remindTime <= now) return@forEachIndexed

            val remindLocal = remindTime.toLocalDateTime(timeZone)
            val content = UNMutableNotificationContent().apply {
                setTitle(course.name.ifBlank { alertTitle })
                setSubtitle(alertTitle)
                setBody(buildString {
                    append(positionLabel)
                    append(": ")
                    append(course.position)
                    if (course.teacher.isNotBlank()) {
                        append("\n")
                        append(teacherLabel)
                        append(": ")
                        append(course.teacher)
                    }
                })
                setSound(UNNotificationSound.defaultSound())
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = remindLocal.toDateComponents(),
                repeats = false
            )
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = notificationIdentifier(ALARM_SLOT_START_ID + index),
                content = content,
                trigger = trigger
            )
            notificationCenter.addNotificationRequest(request, withCompletionHandler = null)
        }
    }

    private fun cancelCourseNotifications() {
        val identifiers = (0 until ALARM_SLOT_COUNT).map { index ->
            notificationIdentifier(ALARM_SLOT_START_ID + index)
        }
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(identifiers)
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(identifiers)
    }

    private suspend fun requestNotificationPermission(): Boolean = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            notificationCenter.requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                completionHandler = { granted, _ -> continuation.resume(granted) }
            )
        }
    }

    private fun notificationIdentifier(slotId: Int): String = "$NOTIFICATION_IDENTIFIER_PREFIX$slotId"

    private fun LocalDateTime.toDateComponents(): NSDateComponents {
        return NSDateComponents().apply {
            year = this@toDateComponents.year.toLong()
            month = (this@toDateComponents.month.ordinal + 1).toLong()
            day = this@toDateComponents.day.toLong()
            hour = this@toDateComponents.hour.toLong()
            minute = this@toDateComponents.minute.toLong()
            second = 0
        }
    }

    private companion object {
        private const val WIDGET_SYNC_DAYS = 7
        private const val ALARM_SLOT_START_ID = 50010
        private const val ALARM_SLOT_COUNT = 101
        private const val NOTIFICATION_IDENTIFIER_PREFIX = "course-reminder-"
    }
}
