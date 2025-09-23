// File: CourseConversionRepository.kt

package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.compose.ui.graphics.toArgb
import androidx.room.Transaction
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeekDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlotDao
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.CourseTableExportModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.CourseTableImportModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.ExportCourseJsonModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.ImportCourseJsonModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.TimeSlotJsonModel
import com.xingheyuzhuan.shiguangschedule.tool.IcsExportTool
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID

class CourseConversionRepository(
    private val courseDao: CourseDao,
    private val courseWeekDao: CourseWeekDao,
    private val timeSlotDao: TimeSlotDao,
    private val appSettingsRepository: AppSettingsRepository
) {
    /**
     * 从一个 JSON 课程列表导入课程，并覆盖指定的现有课表。
     * 此函数主要用于教务系统等只提供课程列表的导入场景。
     *
     * @param tableId 要覆盖的现有课表的 ID。
     * @param coursesJsonModel 包含课程数据的 JSON 列表。
     */
    @Transaction
    suspend fun importCoursesFromList(
        tableId: String,
        coursesJsonModel: List<ImportCourseJsonModel>
    ) {
        courseDao.deleteCoursesByTableId(tableId)

        val courseEntities = mutableListOf<Course>()
        val courseWeekEntities = mutableListOf<CourseWeek>()

        coursesJsonModel.forEach { jsonCourse ->
            val courseId = UUID.randomUUID().toString()
            val courseColor = jsonCourse.color ?: CourseImportExport.getRandomColor().toArgb()

            courseEntities.add(
                Course(
                    id = courseId,
                    courseTableId = tableId,
                    name = jsonCourse.name,
                    teacher = jsonCourse.teacher,
                    position = jsonCourse.position,
                    day = jsonCourse.day,
                    startSection = jsonCourse.startSection,
                    endSection = jsonCourse.endSection,
                    colorInt = courseColor
                )
            )

            jsonCourse.weeks.forEach { week ->
                courseWeekEntities.add(
                    CourseWeek(courseId = courseId, weekNumber = week)
                )
            }
        }

        if (courseEntities.isNotEmpty()) {
            courseDao.insertAll(courseEntities)
        }
        if (courseWeekEntities.isNotEmpty()) {
            courseWeekDao.insertAll(courseWeekEntities)
        }
    }

    /**
     * 从一个完整的 JSON 模型导入课表数据，并覆盖指定的现有课表。
     * 包含课程和时间段。
     *
     * @param tableId 要覆盖的现有课表的 ID。
     * @param courseTableJsonModel 包含课程和时间段的完整 JSON 模型。
     */
    @Transaction
    suspend fun importCourseTableFromJson(
        tableId: String,
        courseTableJsonModel: CourseTableImportModel
    ) {
        courseDao.deleteCoursesByTableId(tableId)
        timeSlotDao.deleteAllTimeSlotsByCourseTableId(tableId)

        val courseEntities = mutableListOf<Course>()
        val courseWeekEntities = mutableListOf<CourseWeek>()
        val timeSlotEntities = mutableListOf<TimeSlot>()

        courseTableJsonModel.courses.forEach { jsonCourse ->
            val courseId = jsonCourse.id ?: UUID.randomUUID().toString()
            val courseColor = jsonCourse.color ?: CourseImportExport.getRandomColor().toArgb()

            courseEntities.add(
                Course(
                    id = courseId,
                    courseTableId = tableId,
                    name = jsonCourse.name,
                    teacher = jsonCourse.teacher,
                    position = jsonCourse.position,
                    day = jsonCourse.day,
                    startSection = jsonCourse.startSection,
                    endSection = jsonCourse.endSection,
                    colorInt = courseColor
                )
            )

            jsonCourse.weeks.forEach { week ->
                courseWeekEntities.add(
                    CourseWeek(courseId = courseId, weekNumber = week)
                )
            }
        }

        courseTableJsonModel.timeSlots.forEach { jsonTimeSlot ->
            timeSlotEntities.add(
                TimeSlot(
                    number = jsonTimeSlot.number,
                    startTime = jsonTimeSlot.startTime,
                    endTime = jsonTimeSlot.endTime,
                    courseTableId = tableId
                )
            )
        }

        if (courseEntities.isNotEmpty()) {
            courseDao.insertAll(courseEntities)
        }
        if (courseWeekEntities.isNotEmpty()) {
            courseWeekDao.insertAll(courseWeekEntities)
        }
        if (timeSlotEntities.isNotEmpty()) {
            timeSlotDao.insertAll(timeSlotEntities)
        }
    }

    /**
     * 将指定课表下的所有数据导出为一个完整的 JSON 模型。
     * 包含课程和时间段。
     *
     * @param tableId 要导出的课表的 ID。
     * @return 包含课程和时间段的完整 JSON 模型。
     */
    suspend fun exportCourseTableToJson(tableId: String): CourseTableExportModel? {

        val coursesWithWeeks = courseDao.getCoursesWithWeeksByTableId(tableId).first()
        val exportCourses = coursesWithWeeks.map { courseWithWeeks ->
            val weeks = courseWithWeeks.weeks.map { it.weekNumber }
            ExportCourseJsonModel(
                id = courseWithWeeks.course.id,
                name = courseWithWeeks.course.name,
                teacher = courseWithWeeks.course.teacher,
                position = courseWithWeeks.course.position,
                day = courseWithWeeks.course.day,
                startSection = courseWithWeeks.course.startSection,
                endSection = courseWithWeeks.course.endSection,
                color = courseWithWeeks.course.colorInt,
                weeks = weeks
            )
        }

        val timeSlots = timeSlotDao.getTimeSlotsByCourseTableId(tableId).first()
        val exportTimeSlots = timeSlots.map { timeSlot ->
            TimeSlotJsonModel(
                number = timeSlot.number,
                startTime = timeSlot.startTime,
                endTime = timeSlot.endTime
            )
        }

        return CourseTableExportModel(
            courses = exportCourses,
            timeSlots = exportTimeSlots
        )
    }

    /**
    * 将指定课表下的所有课程数据导出为 ICS 日历文件的内容字符串。
    *
    * @param tableId 要导出的课表的 ID。
    * @param alarmMinutes 可选的提醒时间，单位分钟。传入null则不设置提醒。
    * @return 包含 ICS 日历文件内容的字符串，如果失败则返回 null。
    */
    suspend fun exportToIcsString(tableId: String, alarmMinutes: Int?): String? {
        val courses = courseDao.getCoursesWithWeeksByTableId(tableId).first()
        val timeSlots = timeSlotDao.getTimeSlotsByCourseTableId(tableId).first()
        val appSettings = appSettingsRepository.getAppSettingsOnce()
        val semesterStartDate = appSettings?.semesterStartDate?.let { LocalDate.parse(it) }

        if (semesterStartDate == null || appSettings.semesterTotalWeeks <= 0) {
            return null
        }

        // 从 appSettings 获取 skippedDates
        val skippedDates = appSettings.skippedDates

        return IcsExportTool.generateIcsFileContent(
            courses = courses,
            timeSlots = timeSlots,
            semesterStartDate = semesterStartDate,
            semesterTotalWeeks = appSettings.semesterTotalWeeks,
            alarmMinutes = alarmMinutes,
            skippedDates = skippedDates
        )
    }
}