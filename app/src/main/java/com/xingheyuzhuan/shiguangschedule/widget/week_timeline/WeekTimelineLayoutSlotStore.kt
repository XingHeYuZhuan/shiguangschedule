package com.xingheyuzhuan.shiguangschedule.widget.week_timeline

import android.content.Context

/**
 * 记录周视图小组件当前使用的布局槽位。
 *
 * 周视图使用两个 ListView 布局交替刷新，避免部分桌面启动器缓存
 * RemoteViewsService 数据导致刷新后仍显示旧内容。
 */
object WeekTimelineLayoutSlotStore {
    private const val PREFS_NAME = "week_timeline_layout_slot_store"
    private const val KEY_PREFIX = "layout_slot_"

    @Synchronized
    fun advance(context: Context, appWidgetId: Int): Int {
        val prefs = prefs(context)
        val current = prefs.getInt(key(appWidgetId), 0).normalize()
        val next = 1 - current
        prefs.edit().putInt(key(appWidgetId), next).apply()
        return next
    }

    @Synchronized
    fun current(context: Context, appWidgetId: Int): Int {
        return prefs(context).getInt(key(appWidgetId), 0).normalize()
    }

    @Synchronized
    fun remove(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        prefs(context).edit().apply {
            appWidgetIds.forEach { remove(key(it)) }
        }.apply()
    }

    @Synchronized
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(appWidgetId: Int) = "$KEY_PREFIX$appWidgetId"

    private fun Int.normalize(): Int = if (this == 1) 1 else 0
}
