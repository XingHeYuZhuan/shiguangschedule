package com.xingheyuzhuan.shiguangschedule.widget.week_timeline

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.RemoteViews
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot

object WeekTimelineNativeRenderer {
    val collectionViewIds = intArrayOf(
        R.id.lv_week_timeline_body_primary,
        R.id.lv_week_timeline_body_secondary
    )

    fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        appWidgetId: Int,
        renderToken: Long = SystemClock.elapsedRealtimeNanos(),
        activeLayoutSlot: Int = (renderToken and 1L).toInt()
    ): RemoteViews {
        val activeIndex = activeLayoutSlot.coerceIn(0, collectionViewIds.lastIndex)
        val activeCollectionId = collectionViewIds[activeIndex]
        val layoutResId = if (activeIndex == 0) {
            R.layout.widget_week_timeline_native
        } else {
            R.layout.widget_week_timeline_native_alt
        }

        val remoteViews = RemoteViews(context.packageName, layoutResId)
        WeekTimelineHeaderBinder.bind(remoteViews, snapshot)

        val clickPendingIntent = createLaunchPendingIntent(context, appWidgetId)
        remoteViews.setOnClickPendingIntent(R.id.widget_root, clickPendingIntent)
        remoteViews.setPendingIntentTemplate(activeCollectionId, clickPendingIntent)

        val serviceIntent = createServiceIntent(
            context = context,
            snapshot = snapshot,
            appWidgetId = appWidgetId,
            renderToken = renderToken,
            collectionSuffix = if (activeIndex == 0) "primary" else "secondary"
        )
        remoteViews.setRemoteAdapter(activeCollectionId, serviceIntent)
        remoteViews.setEmptyView(activeCollectionId, R.id.tv_week_timeline_empty)
        return remoteViews
    }

    private fun createServiceIntent(
        context: Context,
        snapshot: WidgetSnapshot,
        appWidgetId: Int,
        renderToken: Long,
        collectionSuffix: String
    ): Intent {
        return Intent(context, WeekTimelineRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val overrideScale = snapshot.style?.widget_course_font_scale
            if (overrideScale != null && overrideScale > 0f) {
                putExtra(WeekTimelineRemoteViewsService.EXTRA_WIDGET_FONT_SCALE, overrideScale)
            }
            data = Uri.parse(
                toUri(Intent.URI_INTENT_SCHEME) +
                    "#" + appWidgetId +
                    "_" + collectionSuffix +
                    "_" + (overrideScale ?: -1f) +
                    "_" + snapshot.show_weekends +
                    "_" + snapshot.first_day_of_week +
                    "_" + snapshot.hashCode() +
                    "_" + renderToken
            )
        }
    }

    private fun createLaunchPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
