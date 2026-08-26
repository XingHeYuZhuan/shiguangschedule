package com.xingheyuzhuan.shiguangschedule.service

import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.sync.WidgetDataSynchronizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

/**
 * iOS 端同步完成事件的唯一接收器，统一协调通知和 Widget 快照更新。
 */
@Single(createdAtStart = true)
class IosSyncReceiver(
    private val widgetDataSynchronizer: WidgetDataSynchronizer,
    private val styleSettingsRepository: StyleSettingsRepository,
    private val notificationScheduler: IosCourseNotificationScheduler,
    private val snapshotExporter: IosWidgetSnapshotExporter
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            snapshotExporter.exportSnapshot()
            notificationScheduler.rebuildNotifications()

            merge(
                widgetDataSynchronizer.syncCompletedFlow,
                styleSettingsRepository.styleUpdatedFlow
            ).collect {
                snapshotExporter.exportSnapshot()
                notificationScheduler.rebuildNotifications()
            }
        }
    }
}
