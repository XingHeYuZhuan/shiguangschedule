package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xingheyuzhuan.shiguangschedule.data.sync.WidgetDataSynchronizer

/**
 * 负责每15分钟更新一次小组件UI的Worker。
 * 它直接调用所有小组件的UI更新，确保UI及时刷新。
 */
class WidgetUiUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("WidgetSync", "WidgetUiUpdateWorker 开始执行")
        // 调用通用的 UI 更新函数
        updateAllWidgets(applicationContext)
        return Result.success()
    }
}

/**
 * 负责每天执行一次完整数据同步的Worker。
 * 它调用 WidgetDataSynchronizer 的 syncNow() 方法，同步主数据库数据。
 */
class FullDataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    // 通过构造函数注入依赖，与 AppWorkerFactory 匹配
    private val widgetDataSynchronizer: WidgetDataSynchronizer
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("WidgetSync", "FullDataSyncWorker 开始执行")
        try {
            // 直接使用注入的实例
            widgetDataSynchronizer.syncNow()
            return Result.success()
        } catch (e: Exception) {
            // 如果同步失败，可以选择重试或失败
            return Result.failure()
        }
    }
}
