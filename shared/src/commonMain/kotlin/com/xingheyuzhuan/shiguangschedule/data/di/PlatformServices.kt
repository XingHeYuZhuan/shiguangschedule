package com.xingheyuzhuan.shiguangschedule.data.di

import okio.Path

/**
 * 跨平台文件存储路径抽象
 */
interface AppStorage {
    val filesDir: Path
    val cacheDir: Path
    fun getDatabasePath(dbName: String): String
}