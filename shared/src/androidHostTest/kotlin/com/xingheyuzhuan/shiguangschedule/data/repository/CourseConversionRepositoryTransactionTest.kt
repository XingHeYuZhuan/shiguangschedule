package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabaseConstructor
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.CourseConfigJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.CourseTableImportModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.ImportCourseJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.TimeSlotJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFails

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CourseConversionRepositoryTransactionTest {
    private lateinit var database: MainAppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<MainAppDatabase>(
            context = context,
            factory = { MainAppDatabaseConstructor.initialize() }
        )
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun failedWholeTableImportRestoresCoursesWeeksTimeSlotsAndConfig() = runBlocking {
        val tableId = "table"
        val oldCourse = Course(
            id = "old-course",
            courseTableId = tableId,
            name = "原课程",
            teacher = "原教师",
            position = "原教室",
            day = 1,
            startSection = 1,
            endSection = 2,
            isCustomTime = false,
            customStartTime = null,
            customEndTime = null,
            colorInt = 0
        )
        val oldTimeSlot = TimeSlot(1, "08:00", "08:45", tableId)
        val oldConfig = CourseTableConfig(
            courseTableId = tableId,
            semesterStartDate = "2026-02-23",
            semesterTotalWeeks = 18
        )

        database.courseTableDao().insert(CourseTable(tableId, "测试课表", 1L))
        database.courseDao().insertAll(listOf(oldCourse))
        database.courseWeekDao().insertAll(listOf(CourseWeek(oldCourse.id, 1)))
        database.timeSlotDao().insertAll(listOf(oldTimeSlot))
        database.courseTableConfigDao().insertOrUpdate(oldConfig)

        val appSettingsRepository = AppSettingsRepository(
            dataStore = MutableDataStore(emptyPreferences()),
            courseTableDao = database.courseTableDao(),
            courseTableConfigDao = database.courseTableConfigDao()
        )
        val repository = CourseConversionRepository(
            mainAppDatabase = database,
            courseDao = database.courseDao(),
            courseWeekDao = database.courseWeekDao(),
            timeSlotDao = database.timeSlotDao(),
            appSettingsRepository = appSettingsRepository,
            styleSettingsRepository = StyleSettingsRepository(MutableDataStore(ScheduleGridStyleProto()))
        )

        val duplicateIdCourse = ImportCourseJsonModel(
            id = "duplicate-course",
            name = "新课程",
            teacher = "新教师",
            position = "新教室",
            day = 2,
            startSection = 3,
            endSection = 4,
            weeks = listOf(2),
            isCustomTime = false
        )
        val importModel = CourseTableImportModel(
            courses = listOf(duplicateIdCourse, duplicateIdCourse.copy(name = "冲突课程")),
            timeSlots = listOf(TimeSlotJsonModel(1, "09:00", "09:45")),
            config = CourseConfigJsonModel(
                semesterStartDate = "2026-08-24",
                semesterTotalWeeks = 20
            )
        )

        assertFails {
            repository.importCourseTableFromJson(tableId, importModel)
        }

        assertEquals(listOf(oldCourse), database.courseDao().getCoursesByTableId(tableId).first())
        assertEquals(listOf(CourseWeek(oldCourse.id, 1)), database.courseWeekDao().getWeeksByCourseId(oldCourse.id).first())
        assertEquals(listOf(oldTimeSlot), database.timeSlotDao().getTimeSlotsByCourseTableId(tableId).first())
        assertEquals(oldConfig, database.courseTableConfigDao().getConfigOnce(tableId))
    }
}

private class MutableDataStore<T>(initialValue: T) : DataStore<T> {
    private val state = MutableStateFlow(initialValue)

    override val data: Flow<T> = state

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
