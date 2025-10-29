// DoubleDaysScheduleWidget.kt
package com.xingheyuzhuan.shiguangschedule.widget.double_days

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.xingheyuzhuan.shiguangschedule.widget.getWidgetCoursesByDatesAndWeekFlow
import java.time.LocalDate

/**
 * 这是一个 4x2 尺寸的小组件，用于显示今天和明天的课程。
 * 它继承自 GlanceAppWidget，作为小组件的入口点。
 */
class DoubleDaysScheduleWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dates = listOf(LocalDate.now(), LocalDate.now().plusDays(1))
        val combinedFlow = getWidgetCoursesByDatesAndWeekFlow(context, dates)

        provideContent {
            GlanceTheme {
                DoubleDaysLayout(coursesAndWeekFlow = combinedFlow)
            }
        }
    }
}