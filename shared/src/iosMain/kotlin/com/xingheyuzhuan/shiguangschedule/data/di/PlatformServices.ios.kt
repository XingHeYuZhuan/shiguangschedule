package com.xingheyuzhuan.shiguangschedule.data.di

import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Single
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@Single
class IosAppStorage : AppStorage {
    @OptIn(ExperimentalForeignApi::class)
    override val filesDir: Path
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            val pathStr = paths.first() as? String ?: ""
            return pathStr.toPath()
        }

    @OptIn(ExperimentalForeignApi::class)
    override val cacheDir: Path
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            val pathStr = paths.first() as? String ?: ""
            return pathStr.toPath()
        }

    override fun getDatabasePath(dbName: String): String {
        return "$filesDir/$dbName"
    }
}