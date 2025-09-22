package com.xingheyuzhuan.shiguangschedule

import android.app.Application
import androidx.work.Configuration
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetDatabase
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.data.sync.SyncManager
import com.xingheyuzhuan.shiguangschedule.data.sync.WidgetDataSynchronizer
import com.xingheyuzhuan.shiguangschedule.service.AppWorkerFactory

/**
 * 整个应用的全局入口，用于管理所有核心依赖项的单例实例，并为 WorkManager 提供自定义工厂。
 */
class MyApplication : Application(), Configuration.Provider {

    // 主数据库
    val database: MainAppDatabase by lazy { MainAppDatabase.getDatabase(this) }
    // Widget 数据库
    val widgetDatabase: WidgetDatabase by lazy { WidgetDatabase.getDatabase(this) }

    // 主数据库仓库
    val appSettingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository(database.appSettingsDao())
    }
    val timeSlotRepository: TimeSlotRepository by lazy {
        TimeSlotRepository(database.timeSlotDao())
    }
    val courseTableRepository: CourseTableRepository by lazy {
        CourseTableRepository(
            database.courseTableDao(),
            database.courseDao(),
            database.courseWeekDao(),
            timeSlotRepository
        )
    }

    val courseConversionRepository: CourseConversionRepository by lazy {
        CourseConversionRepository(
            database.courseDao(),
            database.courseWeekDao(),
            database.courseTableDao(),
            database.timeSlotDao(),
            appSettingsRepository
        )
    }
    // 新增：Widget 数据库仓库
    val widgetRepository: WidgetRepository by lazy {
        WidgetRepository(
            widgetDatabase.widgetCourseDao(),
            database.appSettingsDao(),
            this
        )
    }

    // 公开 WidgetDataSynchronizer 实例
    val widgetDataSynchronizer by lazy {
        WidgetDataSynchronizer(
            appContext = this,
            appSettingsRepository = appSettingsRepository,
            courseTableRepository = courseTableRepository,
            timeSlotRepository = timeSlotRepository,
            widgetRepository = widgetRepository
        )
    }

    // WorkManager 配置，现在将 widgetDataSynchronizer 传入工厂
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                AppWorkerFactory(
                    appSettingsRepository = appSettingsRepository,
                    widgetRepository = widgetRepository,
                    widgetDataSynchronizer = widgetDataSynchronizer
                )
            )
            .build()

    override fun onCreate() {
        super.onCreate()

        // 1. 触发主数据库的初始化。
        database.courseTableDao()

        // 2. 创建并启动同步管理器。
        val syncManager = SyncManager(
            appContext = this,
            appSettingsRepository = appSettingsRepository,
            courseTableRepository = courseTableRepository,
            timeSlotRepository = timeSlotRepository,
            widgetRepository = widgetRepository
        )
        syncManager.startAllSynchronizers()
    }
}
