package com.xingheyuzhuan.shiguangschedule

import android.app.Application
import androidx.work.Configuration
import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage
import com.xingheyuzhuan.shiguangschedule.data.di.SharedModule
import com.xingheyuzhuan.shiguangschedule.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.plugin.module.dsl.startKoin
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Module(includes = [
    SharedModule::class
])
@ComponentScan("com.xingheyuzhuan.shiguangschedule")
class AppModule

@KoinApplication(modules = [AppModule::class])
class ScheduleAppConfig

/**
 * Android 平台特有的基础依赖注入模块
 */
val androidPlatformModule = module {
    single<AppStorage> {
        object : AppStorage {
            override val filesDir: Path = androidContext().filesDir.absolutePath.toPath()
            override val cacheDir: Path = androidContext().cacheDir.absolutePath.toPath()
            override fun getDatabasePath(dbName: String): String {
                return androidContext().getDatabasePath(dbName).absolutePath
            }
        }
    }
    single(named("AppVersionCode")) { BuildConfig.VERSION_CODE }
    single(named("AppVersionName")) { BuildConfig.VERSION_NAME }
}

class MyApplication : Application(), Configuration.Provider, KoinComponent {

    private val syncManager: SyncManager by inject()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()

        startKoin<ScheduleAppConfig> {
            androidLogger()
            androidContext(this@MyApplication)
            workManagerFactory()
            modules(androidPlatformModule)
        }

        clearShareTempFiles()
        syncManager.startAllSynchronizers()

        CoroutineScope(Dispatchers.IO).launch {
            initOfflineRepo()
        }
    }

    private suspend fun initOfflineRepo() = withContext(Dispatchers.IO) {
        val repoDir = File(filesDir, "repo")
        if (repoDir.exists() && repoDir.list()?.isNotEmpty() == true) return@withContext
        if (!repoDir.exists()) repoDir.mkdirs()
        try {
            copyAssets("offline_repo", repoDir)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun copyAssets(assetPath: String, destDir: File) {
        val assetList = assets.list(assetPath) ?: return
        for (item in assetList) {
            val srcItemPath = "$assetPath/$item"
            val destItem = File(destDir, item)
            try {
                assets.open(srcItemPath).use { input ->
                    FileOutputStream(destItem).use { output -> input.copyTo(output) }
                }
            } catch (e: IOException) {
                destItem.mkdirs()
                copyAssets(srcItemPath, destItem)
            }
        }
    }

    private fun clearShareTempFiles() {
        val shareTempDir = File(cacheDir, "share_temp")
        if (shareTempDir.exists() && shareTempDir.isDirectory) {
            shareTempDir.listFiles()?.forEach { it.delete() }
        }
    }
}