import Foundation
import SwiftData

@Model
final class CourseModel {
    @Attribute(.unique) var id: String
    var courseTable: CourseTableModel?
    var name: String
    var teacher: String
    var position: String
    var day: Int
    var startSection: Int?
    var endSection: Int?
    var isCustomTime: Bool
    var customStartTime: String?
    var customEndTime: String?
    var colorInt: Int
    var remark: String?

    @Relationship(deleteRule: .cascade)
    var weeks: [CourseWeekModel] = []

    init(
        id: String = UUID().uuidString,
        courseTable: CourseTableModel? = nil,
        name: String,
        teacher: String = "",
        position: String = "",
        day: Int,
        startSection: Int? = nil,
        endSection: Int? = nil,
        isCustomTime: Bool = false,
        customStartTime: String? = nil,
        customEndTime: String? = nil,
        colorInt: Int = 0,
        remark: String? = nil
    ) {
        self.id = id
        self.courseTable = courseTable
        self.name = name
        self.teacher = teacher
        self.position = position
        self.day = day
        self.startSection = startSection
        self.endSection = endSection
        self.isCustomTime = isCustomTime
        self.customStartTime = customStartTime
        self.customEndTime = customEndTime
        self.colorInt = colorInt
        self.remark = remark
    }
}

extension CourseModel {
    var weekNumbers: [Int] {
        weeks.map(\.weekNumber).sorted()
    }
}
