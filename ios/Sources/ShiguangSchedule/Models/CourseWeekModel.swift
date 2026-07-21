import Foundation
import SwiftData

@Model
final class CourseWeekModel {
    var course: CourseModel?
    var weekNumber: Int

    init(course: CourseModel? = nil, weekNumber: Int) {
        self.course = course
        self.weekNumber = weekNumber
    }
}
