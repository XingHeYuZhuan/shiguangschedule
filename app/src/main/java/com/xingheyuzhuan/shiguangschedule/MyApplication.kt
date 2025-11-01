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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MyApplication : Application(), Configuration.Provider {

    // 主数据库
    val database: MainAppDatabase by lazy { MainAppDatabase.getDatabase(this) }
    // Widget 数据库
    val widgetDatabase: WidgetDatabase by lazy { WidgetDatabase.getDatabase(this) }

    // 主数据库仓库
    val appSettingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository(
            database.appSettingsDao(),
            database.courseTableConfigDao()
        )
    }
    val timeSlotRepository: TimeSlotRepository by lazy {
        TimeSlotRepository(database.timeSlotDao())
    }
    val courseTableRepository: CourseTableRepository by lazy {
        CourseTableRepository(
            database.courseTableDao(),
            database.courseDao(),
            database.courseWeekDao(),
            timeSlotRepository,
            appSettingsRepository
        )
    }

    val courseConversionRepository: CourseConversionRepository by lazy {
        CourseConversionRepository(
            database.courseDao(),
            database.courseWeekDao(),
            database.timeSlotDao(),
            appSettingsRepository
        )
    }
    // 新增：Widget 数据库仓库
    val widgetRepository: WidgetRepository by lazy {
        WidgetRepository(
            widgetDatabase.widgetCourseDao(),
            widgetDatabase.widgetAppSettingsDao(),
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

        // 在应用启动时清理临时分享文件
        clearShareTempFiles()

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

        // 3. 在应用启动时初始化离线仓库
        CoroutineScope(Dispatchers.IO).launch {
            initOfflineRepo()
        }
    }

    /**
     * 将 assets 目录下的离线仓库资源复制到内部存储，用于首次启动时的初始化。
     */
    private suspend fun initOfflineRepo() = withContext(Dispatchers.IO) {
        val repoDir = File(filesDir, "repo")

        if (repoDir.exists() && repoDir.list()?.isNotEmpty() == true) {
            return@withContext
        }

        if (!repoDir.exists()) {
            repoDir.mkdirs()
        }

        try {
            copyAssets("offline_repo", repoDir)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 递归复制 assets 目录到目标目录。
     */
    private fun copyAssets(assetPath: String, destDir: File) {
        val assetList = assets.list(assetPath) ?: return

        for (item in assetList) {
            val srcItemPath = "$assetPath/$item"
            val destItem = File(destDir, item)

            try {
                assets.open(srcItemPath).use { inputStream ->
                    FileOutputStream(destItem).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: IOException) {
                destItem.mkdirs()
                copyAssets(srcItemPath, destItem)
            }
        }
    }

    /**
     * 清理用于分享的临时文件。
     */
    private fun clearShareTempFiles() {
        // 创建一个指向 "share_temp" 目录的 File 对象
        val shareTempDir = File(cacheDir, "share_temp")
        if (shareTempDir.exists() && shareTempDir.isDirectory) {
            // 如果目录存在，遍历并删除所有文件
            shareTempDir.listFiles()?.forEach { file ->
                file.delete()
            }
        }
    }
}