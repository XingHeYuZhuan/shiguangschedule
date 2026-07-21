import Foundation
import SwiftUI
import Observation

@MainActor
@Observable
final class CourseViewModel {
    var courseTableId: String = ""
    var allCourses: [CourseModel] = []
    var editingCourse: CourseModel? = nil

    // Form fields
    var name: String = ""
    var teacher: String = ""
    var position: String = ""
    var day: Int = 1
    var startSection: Int = 1
    var endSection: Int = 1
    var isCustomTime: Bool = false
    var customStartTime: String = "08:00"
    var customEndTime: String = "09:35"
    var colorInt: Int = 0
    var remark: String = ""
    var selectedWeeks: Set<Int> = []
    var totalWeeks: Int = 20

    var isEditing: Bool { editingCourse != nil }

    func load(for tableId: String) {
        courseTableId = tableId
        allCourses = DatabaseService.shared.getCourses(for: tableId)
        let config = DatabaseService.shared.getConfig(for: tableId)
        totalWeeks = config?.semesterTotalWeeks ?? 20
    }

    func loadCourse(_ course: CourseModel?) {
        editingCourse = course
        if let c = course {
            name = c.name
            teacher = c.teacher
            position = c.position
            day = c.day
            startSection = c.startSection ?? 1
            endSection = c.endSection ?? 2
            isCustomTime = c.isCustomTime
            customStartTime = c.customStartTime ?? "08:00"
            customEndTime = c.customEndTime ?? "09:35"
            colorInt = c.colorInt
            remark = c.remark ?? ""
            selectedWeeks = Set(c.weeks.map(\.weekNumber))
        } else {
            resetForm()
        }
    }

    func resetForm() {
        editingCourse = nil
        name = ""
        teacher = ""
        position = ""
        day = 1
        startSection = 1
        endSection = 1
        isCustomTime = false
        customStartTime = "08:00"
        customEndTime = "09:35"
        colorInt = 0
        remark = ""
        selectedWeeks = []
    }

    func toggleWeek(_ week: Int) {
        if selectedWeeks.contains(week) {
            selectedWeeks.remove(week)
        } else {
            selectedWeeks.insert(week)
        }
    }

    func selectAllWeeks() {
        selectedWeeks = Set(1...totalWeeks)
    }

    func clearWeeks() {
        selectedWeeks = []
    }

    func isOddWeek(_ week: Int) -> Bool { week % 2 == 1 }
    func isEvenWeek(_ week: Int) -> Bool { week % 2 == 0 }

    func selectOddWeeks() {
        selectedWeeks = Set((1...totalWeeks).filter(isOddWeek))
    }

    func selectEvenWeeks() {
        selectedWeeks = Set((1...totalWeeks).filter(isEvenWeek))
    }

    func save() -> Bool {
        guard !name.trimmingCharacters(in: .whitespaces).isEmpty else { return false }
        guard !selectedWeeks.isEmpty else { return false }

        let context = DatabaseService.shared.context
        let course: CourseModel

        if let existing = editingCourse {
            course = existing
        } else {
            course = CourseModel(courseTable: DatabaseService.shared.getCourseTable(by: courseTableId))
            context.insert(course)
        }

        course.name = name
        course.teacher = teacher
        course.position = position
        course.day = day
        course.startSection = isCustomTime ? nil : startSection
        course.endSection = isCustomTime ? nil : endSection
        course.isCustomTime = isCustomTime
        course.customStartTime = isCustomTime ? customStartTime : nil
        course.customEndTime = isCustomTime ? customEndTime : nil
        course.colorInt = colorInt
        course.remark = remark.isEmpty ? nil : remark

        for week in course.weeks {
            context.delete(week)
        }
        course.weeks = selectedWeeks.map { weekNum in
            let cw = CourseWeekModel(course: course, weekNumber: weekNum)
            context.insert(cw)
            return cw
        }

        try? context.save()
        load(for: courseTableId)
        return true
    }

    func deleteCourse(_ course: CourseModel) {
        DatabaseService.shared.deleteCourse(course)
        load(for: courseTableId)
    }

    func duplicateCourse(_ course: CourseModel) {
        let newCourse = CourseModel(
            courseTable: DatabaseService.shared.getCourseTable(by: courseTableId),
            name: "\(course.name)-副本",
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
        try? DatabaseService.shared.context.save()
        load(for: courseTableId)
    }

    var dayNames: [String] {
        ["", "周一", "周二", "周三", "周四", "周五", "周六", "周日"]
    }

    func coursesGroupedByName() -> [(name: String, count: Int, courses: [CourseModel])] {
        let grouped = Dictionary(grouping: allCourses) { $0.name }
        return grouped.map { (name, courses) in
            (name: name, count: courses.count, courses: courses)
        }.sorted { $0.name < $1.name }
    }

    func courseSummary(_ course: CourseModel) -> String {
        var parts: [String] = []
        parts.append(dayNames[safe: course.day] ?? "")
        if let s = course.startSection, let e = course.endSection {
            parts.append("第\(s)-\(e)节")
        }
        if !course.teacher.isEmpty {
            parts.append(course.teacher)
        }
        if !course.position.isEmpty {
            parts.append(course.position)
        }
        let weekStr = weekRangeString(course.weekNumbers)
        parts.append(weekStr)
        return parts.joined(separator: " ")
    }

    private func weekRangeString(_ weeks: [Int]) -> String {
        let sorted = weeks.sorted()
        guard !sorted.isEmpty else { return "" }
        // Try to build ranges
        var result: [String] = []
        var start = sorted[0]
        var end = sorted[0]
        for w in sorted.dropFirst() {
            if w == end + 1 {
                end = w
            } else {
                result.append(start == end ? "\(start)" : "\(start)-\(end)")
                start = w
                end = w
            }
        }
        result.append(start == end ? "\(start)" : "\(start)-\(end)")
        return "第\(result.joined(separator: ","))周"
    }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
