package com.xingheyuzhuan.shiguangschedule.widget.tiny

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import java.time.LocalDate
import java.time.LocalTime

object TinyNativeRenderer {

    fun render(context: Context, snapshot: WidgetSnapshot): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_tiny_native)

        // 状态彻底重置
        resetWidgetState(rv)

        // 设置点击跳转
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        rv.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // 数据准备
        val allCourses = snapshot.courses
        val currentWeek = if (snapshot.current_week <= 0) null else snapshot.current_week
        val now = LocalTime.now()
        val todayStr = LocalDate.now().toString()

        // 状态渲染逻辑

        // 情况 A：假期处理
        if (currentWeek == null) {
            showStatus(rv, context.getString(R.string.title_vacation), context.getString(R.string.widget_vacation_expecting))
            return rv
        }

        // 情况 B：开学期间数据过滤
        val todayAllCourses = allCourses.filter { it.date == todayStr || it.date.isBlank() }
        val nextCourse = todayAllCourses.firstOrNull {
            !it.is_skipped && try {
                LocalTime.parse(it.end_time) > now
            } catch (_: Exception) { true }
        }

        if (nextCourse != null) {
            // 有课显示逻辑
            rv.setViewVisibility(R.id.container_info, View.VISIBLE)
            rv.setViewVisibility(R.id.bubble_frame, View.VISIBLE)
            rv.setViewVisibility(R.id.container_status, View.GONE)

            rv.setTextViewText(R.id.tv_course_name, nextCourse.name)

            val timeText = "${nextCourse.start_time.take(5)} - ${nextCourse.end_time.take(5)}"
            rv.setTextViewText(R.id.tv_course_time, timeText)
            rv.setTextViewText(R.id.tv_course_position, nextCourse.position)

            // 剩余课程数统计 (基于原始列表索引)
            val nextCourseIndex = todayAllCourses.indexOf(nextCourse)
            val remainingCount = todayAllCourses.size - nextCourseIndex
            rv.setTextViewText(R.id.tv_remaining_count, remainingCount.toString())

            // 颜色渲染
            val style = snapshot.style
            val colorInt = nextCourse.color_int
            if (style != null && colorInt < style.course_color_maps.size) {
                val colorPair = style.course_color_maps[colorInt]
                rv.setInt(R.id.bubble_bg_image, "setColorFilter", colorPair.light_color.toInt())
                rv.setInt(R.id.bubble_bg_image_dark, "setColorFilter", colorPair.dark_color.toInt())
            }
        } else {
            // 无课状态
            val tip = if (todayAllCourses.isEmpty()) {
                context.getString(R.string.text_no_courses_today)
            } else {
                context.getString(R.string.widget_today_courses_finished)
            }
            showStatus(rv, tip)
        }

        return rv
    }

    /**
     * 核心优化：每次渲染前强制归零可见性，消除跨状态残留
     */
    private fun resetWidgetState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.container_info, View.GONE)
        rv.setViewVisibility(R.id.bubble_frame, View.GONE)
        rv.setViewVisibility(R.id.container_status, View.GONE)
    }

    private fun showStatus(rv: RemoteViews, title: String, message: String? = null) {
        rv.setViewVisibility(R.id.container_status, View.VISIBLE)
        rv.setTextViewText(R.id.tv_status_title, title)

        if (!message.isNullOrBlank()) {
            rv.setTextViewText(R.id.tv_status_msg, message)
            rv.setViewVisibility(R.id.tv_status_msg, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.tv_status_msg, View.GONE)
        }
    }
}