package com.xingheyuzhuan.shiguangschedule.data.db.main

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Database(
    entities = [
        CourseTable::class,
        Course::class,
        CourseWeek::class,
        TimeSlot::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MainAppDatabase : RoomDatabase() {

    // DAOs
    abstract fun courseTableDao(): CourseTableDao
    abstract fun courseDao(): CourseDao
    abstract fun courseWeekDao(): CourseWeekDao
    abstract fun timeSlotDao(): TimeSlotDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MainAppDatabase? = null

        private val _isInitialized = MutableStateFlow(false)
        val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

        fun getDatabase(context: Context): MainAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainAppDatabase::class.java,
                    "main_app_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 在 IO 线程中执行初始化数据
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    // 1. 初始化默认课表
                                    val defaultCourseTable = CourseTable(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = "我的课表",
                                        createdAt = System.currentTimeMillis()
                                    )
                                    database.courseTableDao().insert(defaultCourseTable)

                                    // 2. 初始化应用设置，并传入默认课表的ID
                                    val defaultSettings = AppSettings(
                                        currentCourseTableId = defaultCourseTable.id
                                    )
                                    database.appSettingsDao().insertOrUpdate(defaultSettings)

                                    // 3. 为默认课表插入默认时间段
                                    val defaultTimeSlots = listOf(
                                        TimeSlot(number = 1, startTime = "08:00", endTime = "08:45", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 2, startTime = "08:50", endTime = "09:35", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 3, startTime = "09:50", endTime = "10:35", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 4, startTime = "10:40", endTime = "11:25", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 5, startTime = "11:30", endTime = "12:15", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 6, startTime = "14:00", endTime = "14:45", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 7, startTime = "14:50", endTime = "15:35", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 8, startTime = "15:45", endTime = "16:30", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 9, startTime = "16:35", endTime = "17:20", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 10, startTime = "18:30", endTime = "19:15", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 11, startTime = "19:20", endTime = "20:05", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 12, startTime = "20:10", endTime = "20:55", courseTableId = defaultCourseTable.id),
                                        TimeSlot(number = 13, startTime = "21:10", endTime = "21:55", courseTableId = defaultCourseTable.id)
                                    )
                                    database.timeSlotDao().insertAll(defaultTimeSlots)

                                    // 在所有数据写入完成后，更新状态为true
                                    _isInitialized.value = true
                                    println("数据库初始化数据已完成写入")
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // 数据库打开时（包括重启时）立即更新状态为true
                            _isInitialized.value = true
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}