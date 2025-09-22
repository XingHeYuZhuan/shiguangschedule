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
class CourseAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "course_notification_channel"
        const val NOTIFICATION_ID_BASE = 1000
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_COURSE_POSITION = "course_position"
        const val EXTRA_COURSE_ID = "course_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->

            val courseName = intent?.getStringExtra(EXTRA_COURSE_NAME) ?: "未知课程"
            val coursePosition = intent?.getStringExtra(EXTRA_COURSE_POSITION) ?: "地点未知"
            val courseIdString = intent?.getStringExtra(EXTRA_COURSE_ID)

            if (!courseIdString.isNullOrEmpty()) {
                val notificationId = courseIdString.hashCode() and 0x7fffffff
                showNotification(ctx, notificationId, courseName, coursePosition)
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
}