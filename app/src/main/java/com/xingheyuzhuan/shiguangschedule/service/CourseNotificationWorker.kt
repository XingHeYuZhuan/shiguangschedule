package com.xingheyuzhuan.shiguangschedule.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import androidx.core.content.edit

/**
 * WorkManager 用于设置课程提醒闹钟的 Worker。
 */
class CourseNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val appSettingsRepository: AppSettingsRepository,
    private val widgetRepository: WidgetRepository
) : CoroutineWorker(appContext, workerParams) {

    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val WIDGET_SYNC_DAYS = 7L
        private const val TAG = "CourseNotificationWorker"

        // SharedPreferences 文件名，用于存储闹钟ID
        private const val ALARM_IDS_PREFS = "alarm_ids_prefs"
        // 存储 ID 集合的键名
        private const val KEY_ACTIVE_ALARM_IDS = "active_alarm_ids"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Worker 任务已启动，正在检查提醒设置...")
        return try {
            val appSettings = appSettingsRepository.getAppSettings().first()

            if (!appSettings.reminderEnabled) {
                Log.i(TAG, "课程提醒功能已关闭，正在取消所有现有闹钟。")
                cancelAllAlarms()
                return Result.success()
            }

            val remindBeforeMinutes = appSettings.remindBeforeMinutes
            Log.i(TAG, "提醒功能已开启，将提前 $remindBeforeMinutes 分钟提醒。")

            val today = LocalDate.now()
            val startDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDate = today.plusDays(WIDGET_SYNC_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE)

            val coursesToRemind = widgetRepository.getWidgetCoursesByDateRange(startDate, endDate).first()
            Log.i(TAG, "获取到 ${coursesToRemind.size} 个未来 $WIDGET_SYNC_DAYS 天的课程。")

            // 使用 SharedPreferences 方案，在设置新闹钟前先取消所有已记录的旧闹钟
            Log.i(TAG, "正在清除所有旧的闹钟...")
            cancelAllAlarms()

            Log.i(TAG, "正在设置新的闹钟...")
            for (course in coursesToRemind) {
                if (course.isSkipped) {
                    Log.i(TAG, "课程 ${course.name} (${course.date}) 已被跳过，不设置提醒。")
                    continue
                }

                val courseDate = LocalDate.parse(course.date)
                val courseTime = LocalTime.parse(course.startTime)
                val courseDateTime = LocalDateTime.of(courseDate, courseTime)
                val remindTime = courseDateTime.minusMinutes(remindBeforeMinutes.toLong())

                if (remindTime.isAfter(LocalDateTime.now())) {
                    setAlarm(course.id, remindTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), course.name, course.position)
                    Log.i(TAG, "已为课程 ${course.name} (${course.date}) 设置提醒。地点: ${course.position}，提醒时间: $remindTime")
                } else {
                    Log.i(TAG, "课程 ${course.name} (${course.date}) 的提醒时间已过，不设置。")
                }
            }

            Log.i(TAG, "所有闹钟设置任务完成。")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "闹钟设置任务失败！", e)
            Result.failure()
        }
    }

    /**
     * 设置一个课程提醒闹钟。
     * 只有在拥有精确闹钟权限时才会设置。
     *
     * 该方法会同时将闹钟ID记录到 SharedPreferences 中。
     */
    private fun setAlarm(courseId: String, triggerTime: Long, name: String, position: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "没有精确闹钟权限，无法设置闹钟。")
                return
            }
        }

        val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_ID, courseId)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_NAME, name)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_POSITION, position)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            abs(courseId.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )

        val sharedPreferences = applicationContext.getSharedPreferences(ALARM_IDS_PREFS, Context.MODE_PRIVATE)
        val currentIds = sharedPreferences.getStringSet(KEY_ACTIVE_ALARM_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentIds.add(courseId)
        sharedPreferences.edit {
            putStringSet(KEY_ACTIVE_ALARM_IDS, currentIds)
        }
        Log.d(TAG, "已将闹钟ID记录到 SharedPreferences: $courseId")
    }

    /**
     * 取消所有由本应用设置的闹钟。
     * 该方法不再依赖课程查询，而是直接从 SharedPreferences 中读取并取消所有已记录的ID。
     */
    private fun cancelAllAlarms() {
        val sharedPreferences = applicationContext.getSharedPreferences(ALARM_IDS_PREFS, Context.MODE_PRIVATE)
        val activeAlarmIds = sharedPreferences.getStringSet(KEY_ACTIVE_ALARM_IDS, null)

        if (activeAlarmIds != null) {
            for (courseId in activeAlarmIds) {
                val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_ID, courseId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
                    abs(courseId.hashCode()),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )

                pendingIntent?.let {
                    alarmManager.cancel(it)
                    it.cancel()
                    Log.d(TAG, "已取消闹钟：${courseId}")
                }
            }
        }

        // 关键一步：取消完所有闹钟后，将记录清空
        sharedPreferences.edit {
            remove(KEY_ACTIVE_ALARM_IDS)
        }
        Log.i(TAG, "所有记录的闹钟已取消，SharedPreferences记录已清空。")
    }
}