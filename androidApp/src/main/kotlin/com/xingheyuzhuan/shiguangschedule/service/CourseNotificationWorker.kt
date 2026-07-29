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
import org.koin.android.annotation.KoinWorker

/**
 * 课程提醒同步服务
 *
 */
@KoinWorker
class CourseNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val appSettingsRepository: AppSettingsRepository,
    private val widgetRepository: WidgetRepository
) : CoroutineWorker(appContext, workerParams) {

    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "CourseNotificationWorker"
        private const val WIDGET_SYNC_DAYS = 7L

        private const val ALARM_SLOT_START_ID = 50010
        private const val ALARM_SLOT_COUNT = 101
    }

    override suspend fun doWork(): Result {
        return try {
            val appSettings = appSettingsRepository.getAppSettings().first()

            // 论开关状态，先清空 50010 - 50110 范围内的所有旧闹钟槽位
            cancelAllAlarms()

            // 如果开关没开，清理完直接结束
            if (!appSettings.reminderEnabled) return Result.success()

            val remindBeforeMinutes = appSettings.remindBeforeMinutes
            val now = LocalDateTime.now()
            val startDate = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDate = now.toLocalDate().plusDays(WIDGET_SYNC_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE)

            // 获取未来课程
            val coursesToRemind = widgetRepository.getWidgetCoursesByDateRange(startDate, endDate).first()
                .filter { !it.isSkipped }

            val zoneId = ZoneId.systemDefault()

            // 按顺序为未来课程分配槽位并设置闹钟
            coursesToRemind.take(ALARM_SLOT_COUNT).forEachIndexed { index, course ->
                val courseDate = LocalDate.parse(course.date)
                val startDT = LocalDateTime.of(courseDate, LocalTime.parse(course.startTime))
                val remindTime = startDT.minusMinutes(remindBeforeMinutes.toLong())

                if (remindTime.isAfter(now)) {
                    setAlarmInternal(
                        requestCode = ALARM_SLOT_START_ID + index,
                        courseId = course.id,
                        triggerTime = remindTime.atZone(zoneId).toInstant().toEpochMilli(),
                        name = course.name,
                        position = course.position,
                        teacher = course.teacher
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "同步课程提醒失败", e)
            Result.failure()
        }
    }

    private fun setAlarmInternal(
        requestCode: Int,
        courseId: String,
        triggerTime: Long,
        name: String,
        position: String,
        teacher: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "缺少精确闹钟权限，无法设置提醒")
            return
        }

        val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
            this.action = "com.xingheyuzhuan.shiguangschedule.ACTION_COURSE_REMIND"
            putExtra(CourseAlarmReceiver.EXTRA_ALARM_SLOT_ID, requestCode)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_ID, courseId)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_NAME, name)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_POSITION, position)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_TEACHER, teacher)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    /**
     * 清理 50010 - 50110 范围内的所有槽位，确保无残留
     */
    private fun cancelAllAlarms() {
        for (i in 0 until ALARM_SLOT_COUNT) {
            val requestCode = ALARM_SLOT_START_ID + i
            val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
                this.action = "com.xingheyuzhuan.shiguangschedule.ACTION_COURSE_REMIND"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
        Log.d(TAG, "已清理课程提醒所有历史槽位 (50010-50110)")
    }
}