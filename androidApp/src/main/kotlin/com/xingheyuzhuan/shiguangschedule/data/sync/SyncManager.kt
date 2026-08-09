package com.xingheyuzhuan.shiguangschedule.data.sync

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.service.CourseNotificationWorker
import com.xingheyuzhuan.shiguangschedule.service.DndSchedulerWorker
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 中心化的同步管理器（Android 端），负责启动共享层的同步，并监听同步完成事件来调度 Worker 与小组件刷新。
 */
@Single(createdAtStart = true)
class SyncManager(
    private val appContext: Context,
    private val widgetDataSynchronizer: WidgetDataSynchronizer,
    private val styleSettingsRepository: StyleSettingsRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isStarted = AtomicBoolean(false)

    // 在构造时自动触发启动逻辑
    init {
        startAllSynchronizers()
    }

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
        if (!isStarted.compareAndSet(false, true)) {
            Log.d("SyncManager", "所有同步器已处于启动状态，跳过重复启动。")
            return
        }

        // 监听数据同步完成
        widgetDataSynchronizer.startSync()
        widgetDataSynchronizer.syncCompletedFlow
            .onEach {
                Log.d("SyncManager", "收到同步完成通知，正在调度 Worker 任务及刷新小组件...")
                triggerNotificationWorker()
                DndSchedulerWorker.enqueueWork(appContext)
                updateAllWidgets(appContext)
            }
            .launchIn(scope)

        // 监听样式更新
        styleSettingsRepository.styleUpdatedFlow
            .onEach {
                Log.d("SyncManager", "收到样式更改通知，正在刷新小组件...")
                updateAllWidgets(appContext)
            }
            .launchIn(scope)

        // 响应式监听应用设置（如通知开关、提前时间、自动模式）的变更
        appSettingsRepository.getAppSettings()
            .map { settings ->
                // 提取影响闹钟和勿扰排程的关键字段组合
                Triple(
                    settings.reminderEnabled to settings.remindBeforeMinutes,
                    settings.autoModeEnabled to settings.autoControlMode,
                    settings.compatWearableSync
                )
            }
            .distinctUntilChanged()
            .onEach {
                Log.d("SyncManager", "检测到通知/自动模式配置变更，正在自动重新调度 Worker...")
                triggerNotificationWorker()
                DndSchedulerWorker.enqueueWork(appContext)
            }
            .launchIn(scope)

        Log.d("SyncManager", "所有平台同步与调度器已启动。")
    }
}