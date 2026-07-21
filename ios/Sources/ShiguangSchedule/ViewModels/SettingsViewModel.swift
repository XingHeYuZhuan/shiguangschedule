import Foundation
import SwiftUI
import Observation

@MainActor
@Observable
final class SettingsViewModel {
    var settings = AppSettings.shared
    var currentTable: CourseTableModel? = nil
    var timeSlots: [TimeSlotModel] = []
    var tables: [CourseTableModel] = []

    var semesterStartDate: Date {
        get {
            let config = currentTable.flatMap { DatabaseService.shared.getConfig(for: $0.id) }
            if let dateStr = config?.semesterStartDate {
                return WeekCalculationService.dateFromString(dateStr) ?? Date()
            }
            return Date()
        }
        set {
            guard let table = currentTable else { return }
            let config = DatabaseService.shared.getConfig(for: table.id) ?? {
                let c = CourseTableConfigModel(courseTable: table)
                DatabaseService.shared.context.insert(c)
                return c
            }()
            config.semesterStartDate = WeekCalculationService.stringFromDate(newValue)
            try? DatabaseService.shared.context.save()
        }
    }

    var semesterTotalWeeks: Int {
        get {
            currentTable.flatMap { DatabaseService.shared.getConfig(for: $0.id)?.semesterTotalWeeks } ?? 20
        }
        set {
            guard let table = currentTable else { return }
            let config = DatabaseService.shared.getConfig(for: table.id) ?? {
                let c = CourseTableConfigModel(courseTable: table)
                DatabaseService.shared.context.insert(c)
                return c
            }()
            config.semesterTotalWeeks = max(1, min(52, newValue))
            try? DatabaseService.shared.context.save()
        }
    }

    var showWeekends: Bool {
        get {
            currentTable.flatMap { DatabaseService.shared.getConfig(for: $0.id)?.showWeekends } ?? false
        }
        set {
            guard let table = currentTable else { return }
            let config = DatabaseService.shared.getConfig(for: table.id) ?? {
                let c = CourseTableConfigModel(courseTable: table)
                DatabaseService.shared.context.insert(c)
                return c
            }()
            config.showWeekends = newValue
            try? DatabaseService.shared.context.save()
        }
    }

    func load() {
        currentTable = DatabaseService.shared.getCurrentCourseTable()
        tables = DatabaseService.shared.getAllCourseTables()
        if let table = currentTable {
            timeSlots = DatabaseService.shared.getTimeSlots(for: table.id)
        }
    }

    func saveTimeSlots(_ slots: [(number: Int, start: String, end: String, alias: String?)]) {
        guard let table = currentTable else { return }
        let models = slots.map {
            TimeSlotModel(number: $0.number, startTime: $0.start, endTime: $0.end, courseTable: table, alias: $0.alias)
        }
        DatabaseService.shared.replaceAllTimeSlots(models, for: table.id)
        load()
    }

    var backupFileName: String {
        WebDAVService.shared.generateBackupFileName()
    }

    func exportBackup() -> Data? {
        guard let table = currentTable else { return nil }
        let courses = DatabaseService.shared.getCourses(for: table.id)
        let slots = DatabaseService.shared.getTimeSlots(for: table.id)
        let config = DatabaseService.shared.getConfig(for: table.id)

        let backup: [String: Any] = [
            "tableId": table.id,
            "tableName": table.name,
            "courses": courses.map { c in
                [
                    "id": c.id, "name": c.name, "teacher": c.teacher,
                    "position": c.position, "day": c.day,
                    "startSection": c.startSection as Any, "endSection": c.endSection as Any,
                    "isCustomTime": c.isCustomTime, "customStartTime": c.customStartTime as Any,
                    "customEndTime": c.customEndTime as Any, "colorInt": c.colorInt,
                    "remark": c.remark as Any,
                    "weeks": c.weeks.map(\.weekNumber),
                ] as [String: Any]
            },
            "timeSlots": slots.map { ["number": $0.number, "startTime": $0.startTime, "endTime": $0.endTime, "alias": $0.alias as Any] as [String: Any] },
            "config": [
                "showWeekends": config?.showWeekends as Any,
                "semesterStartDate": config?.semesterStartDate as Any,
                "semesterTotalWeeks": config?.semesterTotalWeeks as Any,
            ] as [String: Any],
            "exportDate": WeekCalculationService.stringFromDate(Date()),
        ]

        return try? JSONSerialization.data(withJSONObject: backup, options: .prettyPrinted)
    }

    func importBackup(data: Data) -> Bool {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return false }
        guard let tableName = json["tableName"] as? String else { return false }

        let table = CourseTableModel(name: tableName)
        DatabaseService.shared.context.insert(table)

        if let configData = json["config"] as? [String: Any] {
            let config = CourseTableConfigModel(courseTable: table)
            config.showWeekends = configData["showWeekends"] as? Bool ?? false
            config.semesterStartDate = configData["semesterStartDate"] as? String
            config.semesterTotalWeeks = configData["semesterTotalWeeks"] as? Int ?? 20
            DatabaseService.shared.context.insert(config)
            table.config = config
        }

        if let slotsData = json["timeSlots"] as? [[String: Any]] {
            for slotData in slotsData {
                if let number = slotData["number"] as? Int,
                   let start = slotData["startTime"] as? String,
                   let end = slotData["endTime"] as? String {
                    let slot = TimeSlotModel(number: number, startTime: start, endTime: end, courseTable: table)
                    slot.alias = slotData["alias"] as? String
                    DatabaseService.shared.context.insert(slot)
                }
            }
        }

        if let coursesData = json["courses"] as? [[String: Any]] {
            for courseData in coursesData {
                let course = CourseModel(
                    courseTable: table,
                    name: courseData["name"] as? String ?? "",
                    teacher: courseData["teacher"] as? String ?? "",
                    position: courseData["position"] as? String ?? "",
                    day: courseData["day"] as? Int ?? 1,
                    startSection: courseData["startSection"] as? Int,
                    endSection: courseData["endSection"] as? Int,
                    isCustomTime: courseData["isCustomTime"] as? Bool ?? false,
                    customStartTime: courseData["customStartTime"] as? String,
                    customEndTime: courseData["customEndTime"] as? String,
                    colorInt: courseData["colorInt"] as? Int ?? 0,
                    remark: courseData["remark"] as? String
                )
                DatabaseService.shared.context.insert(course)
                if let weeks = courseData["weeks"] as? [Int] {
                    for w in weeks {
                        let cw = CourseWeekModel(course: course, weekNumber: w)
                        DatabaseService.shared.context.insert(cw)
                        course.weeks.append(cw)
                    }
                }
            }
        }

        try? DatabaseService.shared.context.save()
        load()
        return true
    }
}
