// WidgetUpdateHelper.kt
package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyScheduleWidget
import com.xingheyuzhuan.shiguangschedule.widget.compact.TodayScheduleWidget
import com.xingheyuzhuan.shiguangschedule.widget.large.LargeScheduleWidget
import com.xingheyuzhuan.shiguangschedule.widget.moderate.ModerateScheduleWidget

/**
 * 集中管理所有小组件的UI更新。
 * @param context 上下文。
 */
suspend fun updateAllWidgets(context: Context) {
    Log.d("WidgetUpdateHelper", "正在更新所有小组件的UI...")

    // 集中维护需要更新的小组件列表
    val widgetsToUpdate = listOf(
        TinyScheduleWidget(),
        TodayScheduleWidget(),
        ModerateScheduleWidget(),
        LargeScheduleWidget()
    )

    widgetsToUpdate.forEach { widget ->
        widget.updateAll(context)
    }
}