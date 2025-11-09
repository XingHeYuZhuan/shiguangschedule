package com.xingheyuzhuan.shiguangschedule.data.sync

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.service.CourseNotificationWorker
import com.xingheyuzhuan.shiguangschedule.service.DndSchedulerWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

/**
 * 中心化的同步管理器，负责在数据库数据初始化后启动同步任务。
 */
class SyncManager(
    // 仓库实例作为依赖项通过构造函数传入
    private val appContext: Context,
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val widgetRepository: WidgetRepository
) {

    // 使用 SupervisorJob 以便子协程失败不影响其他任务
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

    @OptIn(FlowPreview::class)
    fun startAllSynchronizers() {
        // 监听主数据库的就绪状态
        MainAppDatabase.isInitialized
            .filter { it } // 仅在数据库数据初始化完成后触发
            .onEach {
                // 数据库已就绪，现在安全地创建同步器实例并启动它
                val widgetSynchronizer = WidgetDataSynchronizer(
                    appContext = appContext,
                    appSettingsRepository = appSettingsRepository,
                    courseTableRepository = courseTableRepository,
                    timeSlotRepository = timeSlotRepository,
                    widgetRepository = widgetRepository
                )

                // 启动同步器的监听任务，以应对主数据库数据变化
                widgetSynchronizer.syncFlow.launchIn(scope)

                // 监听 Widget 数据库更新事件，使用 debounce 避免频繁调度 Worker
                widgetRepository.dataUpdatedFlow
                    .debounce(500.milliseconds) // 在 500ms 内只处理一次更新事件
                    .onEach {
                        Log.d("SyncManager", "Widget 数据库数据更新，正在调度 Worker 任务...")

                        triggerNotificationWorker()

                        DndSchedulerWorker.enqueueWork(appContext)
                    }
                    .launchIn(scope)

                println("WidgetDataSynchronizer started.")
                Log.d("SyncManager", "所有同步器已启动。")
            }
            .launchIn(scope)
    }
}