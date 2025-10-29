package com.xingheyuzhuan.shiguangschedule.data.db.widget

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WidgetCourse::class, WidgetAppSettings::class], version = 2, exportSchema = false)
abstract class WidgetDatabase : RoomDatabase() {

    abstract fun widgetCourseDao(): WidgetCourseDao
    abstract fun widgetAppSettingsDao(): WidgetAppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: WidgetDatabase? = null

        fun getDatabase(context: Context): WidgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WidgetDatabase::class.java,
                    "widget_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}