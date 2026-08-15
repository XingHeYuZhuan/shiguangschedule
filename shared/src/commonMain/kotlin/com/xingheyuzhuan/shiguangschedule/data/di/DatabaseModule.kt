package com.xingheyuzhuan.shiguangschedule.data.di

import com.xingheyuzhuan.shiguangschedule.data.db.main.*
import com.xingheyuzhuan.shiguangschedule.data.db.widget.*
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Suppress("unused")
class DatabaseModule {

    @Single
    fun provideMainDatabase(appStorage: AppStorage): MainAppDatabase {
        return MainAppDatabase.getDatabase(appStorage)
    }

    @Single
    fun provideWidgetDatabase(appStorage: AppStorage): WidgetDatabase {
        return WidgetDatabase.getDatabase(appStorage)
    }

    @Factory
    fun provideCourseTableConfigDao(db: MainAppDatabase): CourseTableConfigDao = db.courseTableConfigDao()

    @Factory
    fun provideTimeSlotDao(db: MainAppDatabase): TimeSlotDao = db.timeSlotDao()

    @Factory
    fun provideCourseDao(db: MainAppDatabase): CourseDao = db.courseDao()

    @Factory
    fun provideCourseTableDao(db: MainAppDatabase): CourseTableDao = db.courseTableDao()

    @Factory
    fun provideCourseWeekDao(db: MainAppDatabase): CourseWeekDao = db.courseWeekDao()

    @Factory
    fun provideWidgetCourseDao(db: WidgetDatabase): WidgetCourseDao = db.widgetCourseDao()

    @Factory
    fun provideWidgetAppSettingsDao(db: WidgetDatabase): WidgetAppSettingsDao = db.widgetAppSettingsDao()
}