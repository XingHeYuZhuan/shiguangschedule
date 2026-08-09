package com.xingheyuzhuan.shiguangschedule

import android.app.Application
import androidx.work.Configuration
import com.xingheyuzhuan.shiguangschedule.data.di.SharedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
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

class MyApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()

        startKoin<ScheduleAppConfig> {
            androidLogger()
            androidContext(this@MyApplication)
            workManagerFactory()
        }

        clearShareTempFiles()

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