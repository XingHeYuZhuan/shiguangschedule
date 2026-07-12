package com.xingheyuzhuan.shiguangschedule.widget.week_timeline

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshotLoader
import kotlinx.coroutines.runBlocking

class WeekTimelineRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WeekTimelineRemoteViewsFactory(applicationContext, intent)
    }

    companion object {
        const val EXTRA_WIDGET_FONT_SCALE = "extra_widget_font_scale"
    }
}

private class WeekTimelineRemoteViewsFactory(
    private val context: android.content.Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private val widgetFontScaleOverride: Float? =
        intent.getFloatExtra(WeekTimelineRemoteViewsService.EXTRA_WIDGET_FONT_SCALE, Float.NaN)
            .takeUnless { it.isNaN() }

    private var bodySegments: List<Bitmap> = emptyList()
    private var dataVersion: Long = 0L

    override fun onCreate() {
        loadSnapshot()
    }

    override fun onDataSetChanged() {
        loadSnapshot()
    }

    override fun onDestroy() {
        bodySegments = emptyList()
        dataVersion = 0L
    }

    override fun getCount(): Int {
        return if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) 0 else bodySegments.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        val bodyBitmap = bodySegments.getOrNull(position) ?: return null
        return RemoteViews(context.packageName, R.layout.widget_week_timeline_body_item).apply {
            setImageViewBitmap(R.id.iv_week_timeline_body, bodyBitmap)
            setOnClickFillInIntent(R.id.widget_body_item_root, Intent())
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return (dataVersion shl 32) or (position.toLong() and 0xFFFFFFFFL)
    }

    override fun hasStableIds(): Boolean = false

    private fun loadSnapshot() {
        runBlocking {
            runCatching {
                val snapshot = WidgetSnapshotLoader.load(context, widgetFontScaleOverride)
                val segments = WeekTimelineBodyRenderer.buildSegments(snapshot)
                dataVersion = snapshot.hashCode().toLong() and 0xFFFFFFFFL
                bodySegments = segments.map { segment ->
                    WeekTimelineBodyRenderer.renderSegmentBitmap(snapshot, segment)
                }
                Log.d(
                    "WeekTimelineRVService",
                    "构建周课表分段：${segments.size}段, showWeekends=${snapshot.show_weekends}, dataVersion=$dataVersion"
                )
            }.onFailure { error ->
                bodySegments = emptyList()
                dataVersion = 0L
                Log.e("WeekTimelineRVService", "加载桌面小组件周视图数据失败", error)
            }
        }
    }
}
