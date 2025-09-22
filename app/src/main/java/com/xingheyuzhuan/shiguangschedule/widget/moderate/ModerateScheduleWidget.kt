// ModerateScheduleWidget.kt
package com.xingheyuzhuan.shiguangschedule.widget.moderate

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.xingheyuzhuan.shiguangschedule.widget.getWidgetCoursesAndWeekFlow

/**
 * 这是4x2今日课程小组件的主要逻辑文件。
 * 它负责从 Room 数据库获取数据流，并将其传递给 UI 渲染组件。
 */
class ModerateScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val combinedFlow = getWidgetCoursesAndWeekFlow(context)

        provideContent {
            GlanceTheme {
                ModerateLayout(coursesAndWeekFlow = combinedFlow)
            }
        }
    }
}