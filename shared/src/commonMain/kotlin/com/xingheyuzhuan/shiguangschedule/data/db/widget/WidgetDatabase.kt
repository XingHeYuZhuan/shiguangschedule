package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage

@Database(
    entities = [WidgetCourse::class, WidgetAppSettings::class],
    version = 3,
    exportSchema = false
)
@ConstructedBy(WidgetDatabaseConstructor::class)
abstract class WidgetDatabase : RoomDatabase() {

    abstract fun widgetCourseDao(): WidgetCourseDao
    abstract fun widgetAppSettingsDao(): WidgetAppSettingsDao

    companion object {
        fun getDatabase(appStorage: AppStorage): WidgetDatabase {
            return createWidgetDatabase(appStorage)
        }
    }
}

expect fun createWidgetDatabase(appStorage: AppStorage): WidgetDatabase

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object WidgetDatabaseConstructor : RoomDatabaseConstructor<WidgetDatabase>