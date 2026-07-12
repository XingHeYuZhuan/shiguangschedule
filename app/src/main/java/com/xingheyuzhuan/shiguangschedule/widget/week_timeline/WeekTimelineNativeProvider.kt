package com.xingheyuzhuan.shiguangschedule.widget.week_timeline

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.xingheyuzhuan.shiguangschedule.widget.week_timeline.WeekTimelineLayoutSlotStore
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * 周视图时间轴桌面小组件接收器
 * 负责响应系统刷新广播并触发统一小组件刷新链路。
 */
class WeekTimelineNativeProvider : AppWidgetProvider() {
    private val scope = MainScope()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scope.launch {
            updateAllWidgets(context)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        scope.launch {
            updateAllWidgets(context)
        }
    }


    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WeekTimelineLayoutSlotStore.remove(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManagerHelper.schedulePeriodicWork(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WeekTimelineLayoutSlotStore.clear(context)
        WorkManagerHelper.cancelAllWork(context)
    }
}

