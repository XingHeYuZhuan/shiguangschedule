package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage

actual fun createMainDatabase(appStorage: AppStorage): MainAppDatabase {
    val dbPath = appStorage.getDatabasePath("main_app_database")
    return Room.databaseBuilder<MainAppDatabase>(
        name = dbPath,
        factory = { MainAppDatabaseConstructor.initialize() }
    )
        .addMigrations(*ALL_MIGRATIONS)
        .setDriver(NativeSQLiteDriver())
        .build()
}