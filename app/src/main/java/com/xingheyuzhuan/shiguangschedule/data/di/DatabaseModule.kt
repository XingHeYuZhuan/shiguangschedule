package com.xingheyuzhuan.shiguangschedule.data.di

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfigDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeekDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlotDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettingsDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetDatabase
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Configuration

@Module
@Configuration
@Suppress("unused")
class DatabaseModule {

    // --- 1. 提供数据库全局单例 ---

    @Single
    fun provideMainDatabase(context: Context): MainAppDatabase {
        return MainAppDatabase.getDatabase(context)
    }

    @Single
    fun provideWidgetDatabase(context: Context): WidgetDatabase {
        return WidgetDatabase.getDatabase(context)
    }

    // --- 2. 提供主数据库 (MainAppDatabase) 的 DAO ---

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

    // --- 3. 提供小组件数据库 (WidgetDatabase) 的 DAO ---

    @Factory
    fun provideWidgetCourseDao(db: WidgetDatabase): WidgetCourseDao = db.widgetCourseDao()

    @Factory
    fun provideWidgetAppSettingsDao(db: WidgetDatabase): WidgetAppSettingsDao = db.widgetAppSettingsDao()
}