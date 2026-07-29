package com.xingheyuzhuan.shiguangschedule.data.db.main

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
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
        CourseTableConfig::class
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = MainAppDatabase.RemoveAppSettingsSpec::class)
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MainAppDatabase : RoomDatabase() {

    @androidx.room.DeleteTable(tableName = "app_settings")
    class RemoveAppSettingsSpec : AutoMigrationSpec

    abstract fun courseTableDao(): CourseTableDao
    abstract fun courseDao(): CourseDao
    abstract fun courseWeekDao(): CourseWeekDao
    abstract fun timeSlotDao(): TimeSlotDao
    abstract fun courseTableConfigDao(): CourseTableConfigDao

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
                    .addMigrations(*ALL_MIGRATIONS)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    // 1. 初始化默认课表物理数据
                                    val tableId = java.util.UUID.randomUUID().toString()
                                    val defaultCourseTable = CourseTable(
                                        id = tableId,
                                        name = "我的课表",
                                        createdAt = System.currentTimeMillis()
                                    )
                                    database.courseTableDao().insert(defaultCourseTable)

                                    // 2. 初始化该课表的物理配置模板
                                    val defaultConfig = CourseTableConfig(
                                        courseTableId = tableId,
                                        showWeekends = false,
                                        semesterTotalWeeks = 20,
                                        defaultClassDuration = 45,
                                        defaultBreakDuration = 10,
                                        firstDayOfWeek = 1
                                    )
                                    database.courseTableConfigDao().insertOrUpdate(defaultConfig)

                                    // 注意：不再向 AppSettings 表写入数据，偏好设置初始化交由 DataStore 处理

                                    // 3. 初始化默认时间段
                                    val defaultTimeSlots = listOf(
                                        TimeSlot(number = 1, startTime = "08:00", endTime = "08:45", courseTableId = tableId),
                                        TimeSlot(number = 2, startTime = "08:50", endTime = "09:35", courseTableId = tableId),
                                        TimeSlot(number = 3, startTime = "09:50", endTime = "10:35", courseTableId = tableId),
                                        TimeSlot(number = 4, startTime = "10:40", endTime = "11:25", courseTableId = tableId),
                                        TimeSlot(number = 5, startTime = "11:30", endTime = "12:15", courseTableId = tableId),
                                        TimeSlot(number = 6, startTime = "14:00", endTime = "14:45", courseTableId = tableId),
                                        TimeSlot(number = 7, startTime = "14:50", endTime = "15:35", courseTableId = tableId),
                                        TimeSlot(number = 8, startTime = "15:45", endTime = "16:30", courseTableId = tableId),
                                        TimeSlot(number = 9, startTime = "16:35", endTime = "17:20", courseTableId = tableId),
                                        TimeSlot(number = 10, startTime = "18:30", endTime = "19:15", courseTableId = tableId),
                                        TimeSlot(number = 11, startTime = "19:20", endTime = "20:05", courseTableId = tableId),
                                        TimeSlot(number = 12, startTime = "20:10", endTime = "20:55", courseTableId = tableId),
                                        TimeSlot(number = 13, startTime = "21:10", endTime = "21:55", courseTableId = tableId)
                                    )
                                    database.timeSlotDao().insertAll(defaultTimeSlots)

                                    _isInitialized.value = true
                                    println("数据库初始化数据已完成写入")
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
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