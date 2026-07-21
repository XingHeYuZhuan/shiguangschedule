import Foundation
import SwiftData

@Model
final class CourseTableConfigModel {
    var courseTable: CourseTableModel?
    var showWeekends: Bool
    var semesterStartDate: String?
    var semesterTotalWeeks: Int
    var defaultClassDuration: Int
    var defaultBreakDuration: Int
    var firstDayOfWeek: Int

    init(
        courseTable: CourseTableModel? = nil,
        showWeekends: Bool = false,
        semesterStartDate: String? = nil,
        semesterTotalWeeks: Int = 20,
        defaultClassDuration: Int = 45,
        defaultBreakDuration: Int = 10,
        firstDayOfWeek: Int = 1
    ) {
        self.courseTable = courseTable
        self.showWeekends = showWeekends
        self.semesterStartDate = semesterStartDate
        self.semesterTotalWeeks = semesterTotalWeeks
        self.defaultClassDuration = defaultClassDuration
        self.defaultBreakDuration = defaultBreakDuration
        self.firstDayOfWeek = firstDayOfWeek
    }
}
