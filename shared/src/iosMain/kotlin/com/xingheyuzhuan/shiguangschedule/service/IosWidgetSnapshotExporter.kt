package com.xingheyuzhuan.shiguangschedule.service

import androidx.compose.ui.graphics.toArgb
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import kotlin.time.Clock

@Single(createdAtStart = true)
class IosWidgetSnapshotExporter(
    private val widgetRepository: WidgetRepository,
    private val styleSettingsRepository: StyleSettingsRepository
) {
    private val defaults = NSUserDefaults(suiteName = APP_GROUP_ID)
    private val json = Json { encodeDefaults = true }

    suspend fun exportSnapshot() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val courses = widgetRepository.getWidgetCoursesByDateRange(
            startDate = today.toString(),
            endDate = today.plus(WIDGET_SYNC_DAYS, DateTimeUnit.DAY).toString()
        ).first()
        val settings = widgetRepository.getAppSettingsFlow().first()
        val style = styleSettingsRepository.getStyleOnce()

        val snapshot = IosWidgetSnapshot(
            generatedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
            semesterStartDate = settings?.semesterStartDate,
            semesterTotalWeeks = settings?.semesterTotalWeeks ?: 0,
            firstDayOfWeek = settings?.firstDayOfWeek ?: 1,
            courses = courses.map { course -> course.toSnapshot() },
            colors = style.courseColorMaps.map { color ->
                IosWidgetColor(
                    lightArgb = color.light.toArgb().toHexArgb(),
                    darkArgb = color.dark.toArgb().toHexArgb()
                )
            }
        )

        defaults.setObject(json.encodeToString(snapshot), forKey = SNAPSHOT_KEY)
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = SNAPSHOT_UPDATED_NOTIFICATION,
            `object` = null
        )
    }

    private fun Int.toHexArgb(): String = toUInt().toString(16).padStart(8, '0')

    private fun WidgetCourse.toSnapshot() = IosWidgetCourse(
        id = id,
        name = name,
        teacher = teacher,
        position = position,
        startTime = startTime,
        endTime = endTime,
        isSkipped = isSkipped,
        date = date,
        colorIndex = colorInt
    )

    private companion object {
        private const val APP_GROUP_ID = "group.com.xingheyuzhuan.shiguangschedule"
        private const val SNAPSHOT_KEY = "ios_widget_snapshot"
        private const val SNAPSHOT_UPDATED_NOTIFICATION = "IosWidgetSnapshotUpdated"
        private const val WIDGET_SYNC_DAYS = 7
    }
}

@Serializable
private data class IosWidgetSnapshot(
    val generatedAtEpochMillis: Long,
    val semesterStartDate: String?,
    val semesterTotalWeeks: Int,
    val firstDayOfWeek: Int,
    val courses: List<IosWidgetCourse>,
    val colors: List<IosWidgetColor>
)

@Serializable
private data class IosWidgetCourse(
    val id: String,
    val name: String,
    val teacher: String,
    val position: String,
    val startTime: String,
    val endTime: String,
    val isSkipped: Boolean,
    val date: String,
    val colorIndex: Int
)

@Serializable
private data class IosWidgetColor(
    val lightArgb: String,
    val darkArgb: String
)
