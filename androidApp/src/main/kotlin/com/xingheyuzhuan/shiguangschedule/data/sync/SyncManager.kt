package com.xingheyuzhuan.shiguangschedule.data.sync

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.service.CourseNotificationWorker
import com.xingheyuzhuan.shiguangschedule.service.DndSchedulerWorker
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single

/**
 * 中心化的同步管理器（Android 端），负责启动共享层的同步，并监听同步完成事件来调度 Worker 与小组件刷新。
 */
@Single
class SyncManager(
    private val appContext: Context,
    private val widgetDataSynchronizer: WidgetDataSynchronizer,
    private val styleSettingsRepository: StyleSettingsRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 触发 Worker 的辅助函数
    private fun triggerNotificationWorker() {
        val workRequest = OneTimeWorkRequestBuilder<CourseNotificationWorker>().build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "CourseNotificationWorker_Sync_Update",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * 启动所有同步器及平台相关的后台调度
     */
    fun startAllSynchronizers() {
        widgetDataSynchronizer.startSync()
        widgetDataSynchronizer.syncCompletedFlow
            .onEach {
                Log.d("SyncManager", "收到同步完成通知，正在调度 Worker 任务及刷新小组件...")

                triggerNotificationWorker()
                DndSchedulerWorker.enqueueWork(appContext)
                updateAllWidgets(appContext)
            }
            .launchIn(scope)

        styleSettingsRepository.styleUpdatedFlow
            .onEach {
                Log.d("SyncManager", "收到样式更改通知，正在刷新小组件...")
                updateAllWidgets(appContext)
            }
            .launchIn(scope)

        Log.d("SyncManager", "所有平台同步与调度器已启动。")
    }
}