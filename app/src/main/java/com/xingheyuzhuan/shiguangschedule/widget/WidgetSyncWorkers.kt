package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xingheyuzhuan.shiguangschedule.data.sync.WidgetDataSynchronizer
import org.koin.android.annotation.KoinWorker

/**
 * 负责每15分钟更新一次小组件UI的Worker。
 * 它直接调用所有小组件的UI更新，确保UI及时刷新。
 */
@KoinWorker
class WidgetUiUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("WidgetSync", "WidgetUiUpdateWorker 开始执行")
        updateAllWidgets(applicationContext)
        return Result.success()
    }
}

/**
 * 负责每天执行一次完整数据同步的Worker。
 * 它调用 WidgetDataSynchronizer 的 syncNow() 方法，同步主数据库数据。
 */
@KoinWorker
class FullDataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val widgetDataSynchronizer: WidgetDataSynchronizer
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("WidgetSync", "FullDataSyncWorker 开始执行")
        try {
            widgetDataSynchronizer.syncNow()
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}