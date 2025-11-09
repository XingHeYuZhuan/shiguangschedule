package com.xingheyuzhuan.shiguangschedule.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.net.toUri
import androidx.core.content.getSystemService
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * WorkManager 用于调度上课时行为模式（勿扰/静音）开启/关闭闹钟的 Worker。
 * 核心任务：
 * 1. 检查自动模式开关和权限状态。
 * 2. 即时校准当前模式状态（如果 Worker 在课程进行中被触发）。
 * 3. 取消所有旧的模式闹钟。
 * 4. 计算未来 N 天内最近的模式开启和关闭时间点，并设置精确闹钟。
 */
class DndSchedulerWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val appSettingsRepository: AppSettingsRepository,
    private val widgetRepository: WidgetRepository
) : CoroutineWorker(appContext, workerParams) {

    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = appContext.getSystemService<NotificationManager>()
        ?: throw IllegalStateException("NotificationManager not available")

    companion object {
        const val DND_SCHEDULER_WORK_TAG = "dnd_scheduler_worker_tag"
        private const val TAG = "DndSchedulerWorker"
        // 查找未来 7 天的课程来设置闹钟，保证闹钟不会太远也不会太近
        private const val DND_SCHEDULER_CHECK_DAYS = 7L

        // 使用固定的 Locale.ROOT 保证与 DB 存储格式的一致性
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)

        // 用于生成模式闹钟 PendingIntent 的基础 ID
        private const val DND_ALARM_ID_BASE = 50000

        /**
         * 静态方法：通过 WorkManager 调度 DndSchedulerWorker 运行。
         * 使用 REPLACE 策略确保同一时刻只有一个模式调度任务在排队。
         */
        fun enqueueWork(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<DndSchedulerWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .addTag(DND_SCHEDULER_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                DND_SCHEDULER_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            Log.d(TAG, "上课时自动模式调度器已重新排队。")
        }
    }

    override suspend fun doWork(): Result {
        // 1. 检查自动模式开关状态
        val appSettings = appSettingsRepository.getAppSettings().first()

        if (!appSettings.autoModeEnabled) {
            Log.d(TAG, "上课时自动模式开关已关闭，取消所有模式闹钟。")
            cancelDndAlarms()
            return Result.success()
        }

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "勿扰模式权限（Notification Policy Access）未授予，上课时自动模式功能将无法工作。")
        }

        // 2. 即时状态校准 (防止 Worker 在上课过程中被触发，导致状态错误)
        val shouldBeModeOn = isCurrentlyInDndTime()
        Log.d(TAG, "当前模式状态校准：是否处于课程自动模式时间段内：$shouldBeModeOn")

        // 3. 取消所有旧的模式闹钟，准备重新调度
        cancelDndAlarms()

        // 4. 计算下一个模式开启/关闭时间
        val (nextStartAlarm, nextEndAlarm) = findNextDndAlarmTimes(DND_SCHEDULER_CHECK_DAYS)

        if (nextStartAlarm == null && nextEndAlarm == null) {
            Log.d(TAG, "未来 $DND_SCHEDULER_CHECK_DAYS 天内没有找到课程模式闹钟，终止调度。")
            return Result.success()
        }

        // 5. 设置新的精确闹钟
        nextStartAlarm?.let {
            scheduleDndAlarm(it, isStartAction = true)
            Log.i(TAG, "已设置下一个 [模式开启] 闹钟：${it}")
        }
        nextEndAlarm?.let {
            scheduleDndAlarm(it, isStartAction = false)
            Log.i(TAG, "已设置下一个 [模式关闭] 闹钟：${it}")
        }

        return Result.success()
    }


    /**
     * 查找下一个模式开启和模式关闭的时间点。
     * 开启点是课程的开始时间，关闭点是课程的结束时间。
     */
    private suspend fun findNextDndAlarmTimes(checkDays: Long): Pair<LocalDateTime?, LocalDateTime?> {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val startDate = now.toLocalDate().format(DATE_FORMATTER)
        // 查找未来 N 天的课程，从今天算起
        val endDate = now.toLocalDate().plusDays(checkDays - 1).format(DATE_FORMATTER)

        // 从 Widget 数据库中获取未来 N 天的课程
        val courses = widgetRepository.getWidgetCoursesByDateRange(startDate, endDate).first()
            .filter { !it.isSkipped } // 过滤掉跳过的课程

        var nextStartAlarm: LocalDateTime? = null
        var nextEndAlarm: LocalDateTime? = null

        // 遍历所有课程，找到距离现在最近且在未来的开启和关闭时间
        for (course in courses) {
            val courseDate = LocalDate.parse(course.date, DATE_FORMATTER)

            val startTime = LocalTime.parse(course.startTime)
            val endTime = LocalTime.parse(course.endTime)

            val startDateTime = courseDate.atTime(startTime)
            val endDateTime = courseDate.atTime(endTime)

            if (startDateTime.isAfter(now)) {
                if (nextStartAlarm == null || startDateTime.isBefore(nextStartAlarm)) {
                    nextStartAlarm = startDateTime
                }
            }

            if (endDateTime.isAfter(now)) {
                if (nextEndAlarm == null || endDateTime.isBefore(nextEndAlarm)) {
                    nextEndAlarm = endDateTime
                }
            }
        }

        return Pair(nextStartAlarm, nextEndAlarm)
    }

    /**
     * 检查当前时间是否在任何一节课程的模式时间段内。
     * 用于 Worker 运行时进行即时状态校准：[开始时间, 结束时间)
     */
    private suspend fun isCurrentlyInDndTime(): Boolean {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val todayStr = now.toLocalDate().format(DATE_FORMATTER)

        // 只查询今天的课程
        val courses = widgetRepository.getWidgetCoursesByDateRange(todayStr, todayStr).first()
            .filter { !it.isSkipped }

        val nowTime = now.toLocalTime()

        for (course in courses) {
            val startTime = LocalTime.parse(course.startTime)
            val endTime = LocalTime.parse(course.endTime)

            if (!nowTime.isBefore(startTime) && nowTime.isBefore(endTime)) {
                return true
            }
        }

        return false
    }


    /**
     * 设置一个精确的模式闹钟。
     * @param dateTime 闹钟触发的精确时间
     * @param isStartAction true 表示开启模式 (DND_ACTION_START)，false 表示关闭模式 (DND_ACTION_END)
     */
    private fun scheduleDndAlarm(dateTime: LocalDateTime, isStartAction: Boolean) {
        val triggerTimeMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 使用 CourseAlarmReceiver.DND_ACTION_START/END，Receiver会根据 autoControlMode 决定行为
        val action = if (isStartAction) CourseAlarmReceiver.DND_ACTION_START else CourseAlarmReceiver.DND_ACTION_END

        val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
            // 传递动作类型
            putExtra(CourseAlarmReceiver.EXTRA_DND_ACTION, action)
            data = "DND://${action}".toUri()
        }

        val requestCode = DND_ALARM_ID_BASE + if (isStartAction) 1 else 2

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "无法设置精确闹钟：缺少 SCHEDULE_EXACT_ALARM 权限")
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
    }

    /**
     * 取消所有由模式 Scheduler 设置的闹钟 (开启和关闭闹钟)。
     */
    private fun cancelDndAlarms() {
        // 取消模式开启闹钟 (requestCode: DND_ALARM_ID_BASE + 1)
        val startAction = CourseAlarmReceiver.DND_ACTION_START
        val startIntent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
            putExtra(CourseAlarmReceiver.EXTRA_DND_ACTION, startAction)
            data = "DND://${startAction}".toUri()
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            DND_ALARM_ID_BASE + 1,
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        startPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "已取消上课自动模式开启闹钟。")
        }

        // 取消模式关闭闹钟
        val endAction = CourseAlarmReceiver.DND_ACTION_END
        val endIntent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
            putExtra(CourseAlarmReceiver.EXTRA_DND_ACTION, endAction)
            data = "DND://${endAction}".toUri()
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            DND_ALARM_ID_BASE + 2,
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        endPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "已取消上课自动模式关闭闹钟。")
        }
    }
}