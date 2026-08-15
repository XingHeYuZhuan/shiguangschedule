package com.xingheyuzhuan.shiguangschedule.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyNativeRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// 创建一个局部的注入代理中心，用于在全局顶层方法中安全提取注入实例
private object WidgetDependencyContainer : KoinComponent {
    val repository: WidgetRepository by inject()
    val styleDataStore: DataStore<ScheduleGridStyleProto> by inject()
}

/**
 * 小组件统一分发中心
 * 负责从 Repository 提取数据并分发给所有 4 种规格的原生 Renderer
 */
suspend fun updateAllWidgets(context: Context) {
    try {
        // 1. 从 Koin 容器中动态获取单例化的仓库与 DataStore
        val repository = WidgetDependencyContainer.repository
        val styleDataStore = WidgetDependencyContainer.styleDataStore

        // 2. 准备基础数据
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val dbCourses = withTimeoutOrNull(3.seconds) {
            repository.getWidgetCoursesByDateRange(today.toString(), tomorrow.toString()).first()
        } ?: emptyList()

        val currentWeek = withTimeoutOrNull(2.seconds) {
            repository.getCurrentWeekFlow().first()
        } ?: 0

        val currentStyle = withTimeoutOrNull(2.seconds) {
            styleDataStore.data.first()
        }

        val finalStyleToSync = if (currentStyle == null || currentStyle.course_color_maps.isEmpty()) {
            ScheduleGridStyle.DEFAULT.toProto()
        } else {
            currentStyle
        }

        // 3. 构造数据快照 (Protobuf)
        val courseProtoList = dbCourses.map { course ->
            WidgetCourseProto(
                id = course.id,
                name = course.name,
                teacher = course.teacher,
                position = course.position,
                start_time = course.startTime,
                end_time = course.endTime,
                color_int = course.colorInt,
                is_skipped = course.isSkipped,
                date = course.date
            )
        }

        val snapshot = WidgetSnapshot(
            current_week = currentWeek,
            style = finalStyleToSync,
            courses = courseProtoList
        )

        // 4. 定义所有原生尺寸的映射列表
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val nativeConfigs = listOf(
            TinyNativeProvider::class.java to TinyNativeRenderer::render,
            CompactNativeProvider::class.java to CompactNativeRenderer::render,
            DoubleDaysNativeProvider::class.java to DoubleDaysNativeRenderer::render,
            ListVerticalNativeProvider::class.java to ListVerticalNativeRenderer::render
        )

        // 5. 统一分发更新
        nativeConfigs.forEachIndexed { index, (providerClass, renderFunc) ->
            val componentName = ComponentName(context, providerClass)
            val ids = appWidgetManager.getAppWidgetIds(componentName)

            if (ids.isNotEmpty()) {
                if (index > 0) {
                    delay(300.milliseconds)
                }

                try {
                    val remoteViews = renderFunc(context, snapshot)
                    appWidgetManager.updateAppWidget(componentName, remoteViews)
                    Log.d("WidgetUpdateHelper", "成功刷新规格 ${providerClass.simpleName}")
                } catch (e: Exception) {
                    Log.e("WidgetUpdateHelper", "规格 ${providerClass.simpleName} 渲染失败", e)
                }
            }
        }

    } catch (e: Exception) {
        Log.e("WidgetUpdateHelper", "更新流程异常: ${e.stackTraceToString()}")
    }
}