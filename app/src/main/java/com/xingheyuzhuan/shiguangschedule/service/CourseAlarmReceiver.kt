package com.xingheyuzhuan.shiguangschedule.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.MainActivity
import androidx.core.app.NotificationCompat
import android.util.Log
import androidx.core.content.edit
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "course_notification_channel"
        const val NOTIFICATION_ID_BASE = 1000
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_COURSE_POSITION = "course_position"
        const val EXTRA_COURSE_ID = "course_id"

        private const val ALARM_IDS_PREFS = "alarm_ids_prefs"
        private const val KEY_ACTIVE_ALARM_IDS = "active_alarm_ids"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val courseName = intent?.getStringExtra(EXTRA_COURSE_NAME) ?: "未知课程"
                    val coursePosition = intent?.getStringExtra(EXTRA_COURSE_POSITION) ?: "地点未知"
                    val courseIdString = intent?.getStringExtra(EXTRA_COURSE_ID)

                    if (!courseIdString.isNullOrEmpty()) {
                        val notificationId = courseIdString.hashCode() and 0x7fffffff
                        showNotification(ctx, notificationId, courseName, coursePosition)
                        // 在显示通知后，将对应的闹钟ID从SharedPreferences中移除
                        removeAlarmIdFromPrefs(ctx, courseIdString)

                        // 调用挂起函数更新所有小组件
                        Log.d("CourseAlarmReceiver", "正在触发小组件更新...")
                        updateAllWidgets(ctx)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, courseId: Int, name: String, position: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "课程提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "上课提醒通知"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("上课提醒")
            .setContentText("$name - $position")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val finalNotificationId = NOTIFICATION_ID_BASE + courseId
        notificationManager.notify(finalNotificationId, notification)
    }

    /**
     * 在闹钟触发后，从 SharedPreferences 中移除对应的闹钟 ID。
     * 这确保了已完成的闹钟不会被重复取消，并保持记录的准确性。
     */
    private fun removeAlarmIdFromPrefs(context: Context, courseId: String) {
        val sharedPreferences = context.getSharedPreferences(ALARM_IDS_PREFS, Context.MODE_PRIVATE)
        val currentIds = sharedPreferences.getStringSet(KEY_ACTIVE_ALARM_IDS, null)?.toMutableSet()
        if (currentIds != null) {
            currentIds.remove(courseId)
            // 使用 KTX 扩展函数，代码更简洁
            sharedPreferences.edit {
                putStringSet(KEY_ACTIVE_ALARM_IDS, currentIds)
            }
            Log.d("CourseAlarmReceiver", "已从 SharedPreferences 中移除闹钟ID：$courseId")
        }
    }
}