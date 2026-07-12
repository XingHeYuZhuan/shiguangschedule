package com.xingheyuzhuan.shiguangschedule.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.week_timeline.WeekTimelineLayoutSlotStore
import com.xingheyuzhuan.shiguangschedule.widget.week_timeline.WeekTimelineNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.week_timeline.WeekTimelineNativeRenderer
import kotlinx.coroutines.delay

/**
 * 小组件统一分发中心
 * 负责从 Repository 提取数据并分发给所有 5 种规格的原生 Renderer
 */
suspend fun updateAllWidgets(
    context: Context,
    widgetCourseFontScaleOverride: Float? = null
) {
    try {
        val snapshot = WidgetSnapshotLoader.load(context, widgetCourseFontScaleOverride)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val nativeConfigs = listOf(
            TinyNativeProvider::class.java to TinyNativeRenderer::render,
            CompactNativeProvider::class.java to CompactNativeRenderer::render,
            DoubleDaysNativeProvider::class.java to DoubleDaysNativeRenderer::render,
            ListVerticalNativeProvider::class.java to ListVerticalNativeRenderer::render
        )

        nativeConfigs.forEachIndexed { index, (providerClass, renderFunc) ->
            val componentName = ComponentName(context, providerClass)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isEmpty()) return@forEachIndexed

            if (index > 0) {
                delay(300L)
            }

            try {
                val remoteViews = renderFunc(context, snapshot)
                appWidgetManager.updateAppWidget(componentName, remoteViews)
                Log.d("WidgetUpdateHelper", "成功刷新规格 ${providerClass.simpleName}")
            } catch (error: Exception) {
                Log.e("WidgetUpdateHelper", "规格 ${providerClass.simpleName} 渲染失败", error)
            }
        }

        val weekTimelineComponent = ComponentName(context, WeekTimelineNativeProvider::class.java)
        val weekTimelineIds = appWidgetManager.getAppWidgetIds(weekTimelineComponent)
        if (weekTimelineIds.isNotEmpty()) {
            delay(300L)
            refreshWeekTimelineWidgets(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetIds = weekTimelineIds,
                snapshot = snapshot,
                toggleLayoutSlot = true
            )
            delay(220L)
            val confirmedSnapshot = WidgetSnapshotLoader.load(context, widgetCourseFontScaleOverride)
            refreshWeekTimelineWidgets(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetIds = weekTimelineIds,
                snapshot = confirmedSnapshot,
                toggleLayoutSlot = false
            )
            Log.d(
                "WidgetUpdateHelper",
                "成功刷新规格 ${WeekTimelineNativeProvider::class.java.simpleName}，showWeekends=${confirmedSnapshot.show_weekends}"
            )
        }
    } catch (error: Exception) {
        Log.e("WidgetUpdateHelper", "更新流程异常: ${error.stackTraceToString()}")
    }
}

private fun refreshWeekTimelineWidgets(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
    snapshot: WidgetSnapshot,
    toggleLayoutSlot: Boolean
) {
    val renderToken = SystemClock.elapsedRealtimeNanos()
    WeekTimelineNativeRenderer.collectionViewIds.forEach { collectionViewId ->
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, collectionViewId)
    }
    appWidgetIds.forEach { appWidgetId ->
        try {
            val activeLayoutSlot = if (toggleLayoutSlot) {
                WeekTimelineLayoutSlotStore.advance(context, appWidgetId)
            } else {
                WeekTimelineLayoutSlotStore.current(context, appWidgetId)
            }
            val remoteViews = WeekTimelineNativeRenderer.render(
                context = context,
                snapshot = snapshot,
                appWidgetId = appWidgetId,
                renderToken = renderToken,
                activeLayoutSlot = activeLayoutSlot
            )
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            Log.d(
                "WidgetUpdateHelper",
                "周视图小组件刷新 appWidgetId=$appWidgetId, showWeekends=${snapshot.show_weekends}, hash=${snapshot.hashCode()}, renderToken=$renderToken"
            )
        } catch (error: Exception) {
            Log.e("WidgetUpdateHelper", "周视图小组件渲染失败，appWidgetId=$appWidgetId", error)
        }
    }
    WeekTimelineNativeRenderer.collectionViewIds.forEach { collectionViewId ->
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, collectionViewId)
    }
}
