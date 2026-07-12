package com.xingheyuzhuan.shiguangschedule.widget

import android.util.TypedValue
import android.widget.RemoteViews

private const val DEFAULT_WIDGET_COURSE_FONT_SCALE = 1f

fun WidgetSnapshot.widgetCourseFontScale(): Float {
    return style?.widget_course_font_scale
        ?.takeIf { it > 0f }
        ?.coerceIn(0.5f, 2.0f)
        ?: DEFAULT_WIDGET_COURSE_FONT_SCALE
}

fun RemoteViews.setScaledTextSize(viewId: Int, baseSp: Float, scale: Float) {
    setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, baseSp * scale)
}
