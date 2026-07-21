import Foundation
import SwiftUI
import SwiftData
import Observation

@MainActor
@Observable
final class ScheduleViewModel {
    var currentWeek: Int = 1
    var totalWeeks: Int = 20
    var semesterStartDate: String? = nil
    var firstDayOfWeek: Int = 1
    var showWeekends: Bool = false
    var currentTable: CourseTableModel? = nil
    var courses: [CourseModel] = []
    var timeSlots: [TimeSlotModel] = []
    var holidays: [String: Bool] = [:]
    var courseColorMap: [Int: Color] = [:]

    var weekLabel: String {
        "第\(currentWeek)周 / 共\(totalWeeks)周"
    }

    func loadCurrentTable() {
        currentTable = DatabaseService.shared.getCurrentCourseTable()
        guard let table = currentTable else { return }

        let config = DatabaseService.shared.getConfig(for: table.id)
        semesterStartDate = config?.semesterStartDate
        totalWeeks = config?.semesterTotalWeeks ?? 20
        firstDayOfWeek = config?.firstDayOfWeek ?? 1
        showWeekends = config?.showWeekends ?? false

        currentWeek = WeekCalculationService.currentWeekNumber(semesterStartDate: semesterStartDate)
        if currentWeek > totalWeeks { currentWeek = totalWeeks }

        courses = DatabaseService.shared.getCourses(for: table.id)
        timeSlots = DatabaseService.shared.getTimeSlots(for: table.id)
    }

    func coursesForWeek(_ weekNumber: Int) -> [CourseModel] {
        courses.filter { course in
            course.weeks.contains { $0.weekNumber == weekNumber }
        }
    }

    func coursesForDay(day: Int, week: Int) -> [CourseModel] {
        courses.filter { course in
            course.day == day && course.weeks.contains { $0.weekNumber == week }
        }
    }

    func timeString(for slot: TimeSlotModel) -> String {
        "\(slot.startTime)-\(slot.endTime)"
    }

    func changeWeek(to week: Int) {
        guard week >= 1, week <= totalWeeks else { return }
        currentWeek = week
    }

    func nextWeek() { changeWeek(to: currentWeek + 1) }
    func previousWeek() { changeWeek(to: currentWeek - 1) }

    func refresh() {
        loadCurrentTable()
    }
}
