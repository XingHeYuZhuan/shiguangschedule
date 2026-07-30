package com.xingheyuzhuan.shiguangschedule.data.di

import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Single
import java.io.File

@Single
class JvmAppStorage : AppStorage {
    private val appRootDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".ShiguangSchedule")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    override val filesDir: Path
        get() {
            val dir = File(appRootDir, "files")
            if (!dir.exists()) dir.mkdirs()
            return dir.absolutePath.toPath()
        }

    override val cacheDir: Path
        get() {
            val dir = File(appRootDir, "cache")
            if (!dir.exists()) dir.mkdirs()
            return dir.absolutePath.toPath()
        }

    override fun getDatabasePath(dbName: String): String {
        val dbDir = File(appRootDir, "databases")
        if (!dbDir.exists()) dbDir.mkdirs()
        return File(dbDir, dbName).absolutePath
    }
}