package com.xingheyuzhuan.shiguangschedule.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService // 引入 getSystemService 扩展函数
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.MyApplication // 导入您的 Application 类
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first // 导入 flow.first
import kotlinx.coroutines.launch

class CourseAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "course_notification_channel"
        const val NOTIFICATION_ID_BASE = 1000
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_COURSE_POSITION = "course_position"
        const val EXTRA_COURSE_ID = "course_id"

        const val EXTRA_DND_ACTION = "extra_dnd_action" // Intent 中用于传递动作类型的 Key
        const val DND_ACTION_START = "dnd_action_start" // 模式开启的动作值
        const val DND_ACTION_END = "dnd_action_end"     // 模式关闭的动作值

        private const val ALARM_IDS_PREFS = "alarm_ids_prefs"
        private const val KEY_ACTIVE_ALARM_IDS = "active_alarm_ids"

        private const val TAG = "CourseAlarmReceiver"

        const val MODE_DND = "DND"
        const val MODE_SILENT = "SILENT"

        /**
         * 核心功能：切换上课时行为模式 (勿扰/静音)
         * @param context Context
         * @param enableMode true 表示开启模式，false 表示关闭模式
         * @param modeType 决定要切换的模式类型 ("DND" 或 "SILENT")
         */
        fun toggleMode(context: Context, enableMode: Boolean, modeType: String) {
            val audioManager = context.getSystemService<AudioManager>()
            val notificationManager = context.getSystemService<NotificationManager>()

            if (audioManager == null || notificationManager == null) {
                Log.e(TAG, "无法获取 AudioManager 或 NotificationManager 服务。")
                return
            }

            // 勿扰模式权限是 DND 和静音模式都需要的基础权限
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Log.w(TAG, "缺少勿扰模式权限，无法切换自动行为模式状态。")
                return
            }

            val logPrefix = if (enableMode) "开启" else "关闭"

            when (modeType) {
                MODE_DND -> {
                    if (enableMode) {
                        // DND 开启: 完全静音 (Total Silence)
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                        Log.i(TAG, "自动模式：已${logPrefix} [勿扰模式] (INTERRUPTION_FILTER_NONE)。")
                    } else {
                        // DND 关闭: 恢复所有通知
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        Log.i(TAG, "自动模式：已${logPrefix} [勿扰模式] (INTERRUPTION_FILTER_ALL)。")
                    }
                }
                MODE_SILENT -> {
                    if (enableMode) {
                        // 静音模式开启 (Silent)
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        Log.i(TAG, "自动模式：已${logPrefix} [静音模式] (RINGER_MODE_SILENT)。")
                    } else {
                        // 静音模式关闭 (恢复正常模式)
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        Log.i(TAG, "自动模式：已${logPrefix} [静音模式] (RINGER_MODE_NORMAL)。")
                    }
                }
                else -> {
                    Log.e(TAG, "未知或不支持的自动模式类型: $modeType")
                }
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = ctx.applicationContext as? MyApplication
                    val appSettingsRepository = app?.appSettingsRepository

                    if (appSettingsRepository == null) {
                        Log.e(TAG, "无法获取 MyApplication 实例或 AppSettingsRepository。")
                        return@launch
                    }

                    val appSettings = appSettingsRepository.getAppSettings().first()
                    val modeToUse = appSettings.autoControlMode

                    // 检查模式动作
                    val dndAction = intent?.getStringExtra(EXTRA_DND_ACTION)

                    if (!dndAction.isNullOrEmpty()) {
                        // 1. 模式闹钟被触发
                        if (dndAction == DND_ACTION_START) {
                            toggleMode(ctx, true, modeToUse)
                        } else if (dndAction == DND_ACTION_END) {
                            toggleMode(ctx, false, modeToUse)

                            DndSchedulerWorker.enqueueWork(ctx)
                        }

                    } else {
                        val courseName = intent?.getStringExtra(EXTRA_COURSE_NAME) ?: "未知课程"
                        val coursePosition = intent?.getStringExtra(EXTRA_COURSE_POSITION) ?: "地点未知"
                        val courseIdString = intent?.getStringExtra(EXTRA_COURSE_ID)

                        if (!courseIdString.isNullOrEmpty()) {
                            val notificationId = courseIdString.hashCode() and 0x7fffffff
                            showNotification(ctx, notificationId, courseName, coursePosition)
                            removeAlarmIdFromPrefs(ctx, courseIdString)

                            Log.d(TAG, "正在触发小组件更新...")
                            updateAllWidgets(ctx)
                        }
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

        // 1. 获取彩色大图标的 Bitmap
        val largeIconBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.mipmap.ic_launcher
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIconBitmap)
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
            Log.d(TAG, "已从 SharedPreferences 中移除闹钟ID：$courseId")
        }
    }
}