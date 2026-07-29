package com.xingheyuzhuan.shiguangschedule.widget.list_vertical

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * 垂直列表版小组件接收器
 * 负责响应系统刷新广播并触发 ListVerticalNativeRenderer 进行渲染
 */
class ListVerticalNativeProvider : AppWidgetProvider() {
    private val scope = MainScope()

    /**
     * 当小组件需要更新时调用（系统定时或手动请求）
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scope.launch {
            updateAllWidgets(context)
        }
    }

    /**
     * 当第一个该类型的小组件添加到桌面时调用
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 确保后台任务正在运行
        WorkManagerHelper.schedulePeriodicWork(context)
    }

    /**
     * 当该类型的最后一个小组件从桌面移除时调用
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManagerHelper.cancelAllWork(context)
    }
}