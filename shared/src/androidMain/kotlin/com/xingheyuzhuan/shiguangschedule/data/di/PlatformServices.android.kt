package com.xingheyuzhuan.shiguangschedule.data.di

import android.content.Context
import okio.Path
import okio.Path.Companion.toOkioPath
import org.koin.core.annotation.Single

@Single
class AndroidAppStorage(private val context: Context) : AppStorage {
    override val filesDir: Path
        get() = context.filesDir.toOkioPath()

    override val cacheDir: Path
        get() = context.cacheDir.toOkioPath()

    override fun getDatabasePath(dbName: String): String {
        return context.getDatabasePath(dbName).absolutePath
    }
}