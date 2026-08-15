package com.xingheyuzhuan.shiguangschedule.data.db.main

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage
import org.koin.mp.KoinPlatform

actual fun createMainDatabase(appStorage: AppStorage): MainAppDatabase {
    val context = KoinPlatform.getKoin().get<Context>()
    val dbPath = appStorage.getDatabasePath("main_app_database")
    return Room.databaseBuilder<MainAppDatabase>(
        context = context,
        name = dbPath,
        factory = { MainAppDatabaseConstructor.initialize() }
    )
        .addMigrations(*ALL_MIGRATIONS)
        .setDriver(AndroidSQLiteDriver())
        .build()
}