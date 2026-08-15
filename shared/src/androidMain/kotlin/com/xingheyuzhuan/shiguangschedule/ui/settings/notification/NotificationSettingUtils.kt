package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri

// --- 权限检查逻辑 ---

/**
 * 检查是否拥有精确闹钟调度权限
 */
fun hasExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService<AlarmManager>()
        alarmManager?.canScheduleExactAlarms() ?: false
    } else {
        true
    }
}

/**
 * 检查是否拥有发送通知权限 (Android 13+)
 */
fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

/**
 * 检查是否拥有勿扰模式 (DND) 控制权限
 */
fun hasDndPermission(context: Context): Boolean {
    val notificationManager = context.getSystemService<NotificationManager>()
    return notificationManager?.isNotificationPolicyAccessGranted ?: false
}

// --- 系统设置页面跳转 ---

/**
 * 打开精确闹钟权限设置页
 */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safelyStartActivity(context, intent) { openAppSettings(context) }
    } else {
        openAppSettings(context)
    }
}

/**
 * 打开勿扰权限设置页
 */
fun openDndSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    safelyStartActivity(context, intent) { openAppSettings(context) }
}

/**
 * 打开当前应用的系统设置详情页
 */
fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        "package:${context.packageName}".toUri()
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    safelyStartActivity(context, intent)
}

/**
 * 打开忽略电池优化设置页
 */
fun openIgnoreBatteryOptimizationSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        "package:${context.packageName}".toUri()
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    safelyStartActivity(context, intent) { openAppSettings(context) }
}

/**
 * 安全启动 Activity，防止由于系统裁剪导致 ActivityNotFoundException
 */
private inline fun safelyStartActivity(
    context: Context,
    intent: Intent,
    onFallback: () -> Unit = {}
) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        onFallback()
    }
}