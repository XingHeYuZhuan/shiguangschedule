// ModerateScheduleWidgetReceiver.kt
package com.xingheyuzhuan.shiguangschedule.widget.moderate

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper

class ModerateScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ModerateScheduleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 在第一个小组件实例被添加到主屏幕时，调度后台任务
        WorkManagerHelper.schedulePeriodicWork(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 当最后一个小组件实例被移除时，取消所有后台任务
        WorkManagerHelper.cancelAllWork(context)
    }
}