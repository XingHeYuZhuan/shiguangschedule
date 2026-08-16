package com.xingheyuzhuan.shiguangschedule.widget.compact

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * 今日课程（紧凑版）原生小组件接收器
 */
class CompactNativeProvider : AppWidgetProvider() {
    private val scope = MainScope()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scope.launch {
            // 触发全局刷新逻辑
            updateAllWidgets(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 启动定时任务（15分钟 UI 刷新 + 24小时数据同步）
        WorkManagerHelper.schedulePeriodicWork(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 只有在确定不再需要后台更新时才取消
        WorkManagerHelper.cancelAllWork(context)
    }
}