package com.xingheyuzhuan.shiguangschedule.widget.large

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.xingheyuzhuan.shiguangschedule.widget.getWidgetCoursesByDatesAndWeekFlow
import java.time.LocalDate

/**
 * 这是4x4大尺寸课程小组件的主要逻辑文件。
 * 它负责获取数据流并将其传递给 UI 渲染组件。
 */
class LargeScheduleWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val combinedFlow = getWidgetCoursesByDatesAndWeekFlow(context, listOf(today, tomorrow))

        provideContent {
            GlanceTheme {
                LargeLayout(multiDayCoursesAndWeekFlow = combinedFlow)
            }
        }
    }
}