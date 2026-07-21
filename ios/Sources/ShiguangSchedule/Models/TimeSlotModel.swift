import Foundation
import SwiftData

@Model
final class TimeSlotModel {
    var number: Int
    var startTime: String
    var endTime: String
    var courseTable: CourseTableModel?
    var alias: String?

    init(number: Int, startTime: String, endTime: String, courseTable: CourseTableModel? = nil, alias: String? = nil) {
        self.number = number
        self.startTime = startTime
        self.endTime = endTime
        self.courseTable = courseTable
        self.alias = alias
    }
}
