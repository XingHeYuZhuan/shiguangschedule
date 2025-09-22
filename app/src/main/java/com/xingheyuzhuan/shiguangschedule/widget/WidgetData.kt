// WidgetData.kt
package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/**
 * 提供所有小组件所需数据的通用数据流。
 * @param context 上下文，用于获取 Repository 实例。
 * @return 一个包含 (课程列表, 当前周数) 的数据流。
 */
fun getWidgetCoursesAndWeekFlow(context: Context): Flow<Pair<List<WidgetCourse>, Int?>> {
    val application = context.applicationContext as MyApplication
    val widgetRepository = application.widgetRepository

    val coursesFlow = widgetRepository.getWidgetCoursesByDateRange(
        LocalDate.now().toString(),
        LocalDate.now().toString()
    )
    val currentWeekFlow = widgetRepository.getCurrentWeekFlow()

    return coursesFlow.combine(currentWeekFlow) { courses, currentWeek ->
        Pair(courses, currentWeek)
    }
}

fun getWidgetCoursesByDatesAndWeekFlow(
    context: Context,
    dates: List<LocalDate>
): Flow<Pair<List<List<WidgetCourse>>, Int?>> {
    val application = context.applicationContext as MyApplication
    val widgetRepository = application.widgetRepository

    val coursesFlows = dates.map { date ->
        widgetRepository.getWidgetCoursesByDateRange(date.toString(), date.toString())
    }

    val combinedCoursesFlow = combine(coursesFlows) { coursesArrays ->
        coursesArrays.map { it.toList() }
    }

    val currentWeekFlow = widgetRepository.getCurrentWeekFlow()

    return combinedCoursesFlow.combine(currentWeekFlow) { coursesList, currentWeek ->
        Pair(coursesList, currentWeek)
    }
}