import Foundation
import SwiftUI
import Observation

@MainActor
@Observable
final class CourseTableManagerViewModel {
    var tables: [CourseTableModel] = []
    var currentTableId: String = ""

    func load() {
        tables = DatabaseService.shared.getAllCourseTables()
        currentTableId = AppSettings.shared.currentCourseTableId
    }

    func switchToTable(_ id: String) {
        AppSettings.shared.currentCourseTableId = id
        currentTableId = id
    }

    func createTable(name: String) {
        let table = CourseTableModel(name: name)
        DatabaseService.shared.context.insert(table)

        let config = CourseTableConfigModel(courseTable: table)
        DatabaseService.shared.context.insert(config)
        table.config = config

        try? DatabaseService.shared.context.save()
        load()
    }

    func renameTable(_ table: CourseTableModel, newName: String) {
        table.name = newName
        try? DatabaseService.shared.context.save()
        load()
    }

    func deleteTable(_ table: CourseTableModel) {
        let isCurrent = AppSettings.shared.currentCourseTableId == table.id
        DatabaseService.shared.deleteCourseTable(table)
        if isCurrent {
            let remaining = DatabaseService.shared.getAllCourseTables()
            if let first = remaining.first {
                AppSettings.shared.currentCourseTableId = first.id
            }
        }
        load()
    }

    func duplicateTable(_ table: CourseTableModel) {
        let newTable = CourseTableModel(name: "\(table.name)-副本")
        DatabaseService.shared.context.insert(newTable)

        if let config = DatabaseService.shared.getConfig(for: table.id) {
            let newConfig = CourseTableConfigModel(
                courseTable: newTable,
                showWeekends: config.showWeekends,
                semesterStartDate: config.semesterStartDate,
                semesterTotalWeeks: config.semesterTotalWeeks,
                defaultClassDuration: config.defaultClassDuration,
                defaultBreakDuration: config.defaultBreakDuration,
                firstDayOfWeek: config.firstDayOfWeek
            )
            DatabaseService.shared.context.insert(newConfig)
            newTable.config = newConfig
        }

        let slots = DatabaseService.shared.getTimeSlots(for: table.id)
        for slot in slots {
            let newSlot = TimeSlotModel(
                number: slot.number,
                startTime: slot.startTime,
                endTime: slot.endTime,
                courseTable: newTable,
                alias: slot.alias
            )
            DatabaseService.shared.context.insert(newSlot)
        }

        let courses = DatabaseService.shared.getCourses(for: table.id)
        for course in courses {
            let newCourse = CourseModel(
                courseTable: newTable,
                name: course.name,
                teacher: course.teacher,
                position: course.position,
                day: course.day,
                startSection: course.startSection,
                endSection: course.endSection,
                isCustomTime: course.isCustomTime,
                customStartTime: course.customStartTime,
                customEndTime: course.customEndTime,
                colorInt: course.colorInt,
                remark: course.remark
            )
            DatabaseService.shared.context.insert(newCourse)
            for week in course.weeks {
                let cw = CourseWeekModel(course: newCourse, weekNumber: week.weekNumber)
                DatabaseService.shared.context.insert(cw)
                newCourse.weeks.append(cw)
            }
        }

        try? DatabaseService.shared.context.save()
        load()
    }
}
