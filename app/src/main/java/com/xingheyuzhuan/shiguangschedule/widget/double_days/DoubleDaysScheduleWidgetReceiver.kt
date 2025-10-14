// DoubleDaysScheduleWidgetReceiver.kt
package com.xingheyuzhuan.shiguangschedule.widget.double_days

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper

class DoubleDaysScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DoubleDaysScheduleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 当第一个小组件实例被添加时，调度定期任务
        WorkManagerHelper.schedulePeriodicWork(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 当最后一个小组件实例被移除时，取消所有定期任务
        WorkManagerHelper.cancelAllWork(context)
    }
}