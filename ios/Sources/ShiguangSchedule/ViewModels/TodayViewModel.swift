import Foundation
import SwiftUI
import Observation

@MainActor
@Observable
final class TodayViewModel {
    var currentTable: CourseTableModel? = nil
    var todayCourses: [CourseModel] = []
    var timeSlots: [TimeSlotModel] = []
    var isHoliday: Bool = false
    var currentWeek: Int = 1
    var todayDay: Int = 1

    func load() {
        currentTable = DatabaseService.shared.getCurrentCourseTable()
        guard let table = currentTable else { return }

        let config = DatabaseService.shared.getConfig(for: table.id)
        let semesterStart = config?.semesterStartDate
        firstDayOfWeek = config?.firstDayOfWeek ?? 1

        currentWeek = WeekCalculationService.currentWeekNumber(semesterStartDate: semesterStart)
        todayDay = WeekCalculationService.currentDayOfWeek(firstDayOfWeek: firstDayOfWeek)

        let allCourses = DatabaseService.shared.getCourses(for: table.id)
        todayCourses = allCourses.filter { course in
            course.day == todayDay && course.weeks.contains { $0.weekNumber == currentWeek }
        }.sorted { a, b in
            (a.startSection ?? 99) < (b.startSection ?? 99)
        }

        timeSlots = DatabaseService.shared.getTimeSlots(for: table.id)

        let dateStr = WeekCalculationService.stringFromDate(Date())
        let settings = AppSettings.shared
        isHoliday = settings.skippedDates.contains(dateStr)
    }

    var firstDayOfWeek: Int = 1

    var todayDayName: String {
        let names = ["", "周一", "周二", "周三", "周四", "周五", "周六", "周日"]
        return todayDay >= 1 && todayDay <= 7 ? names[todayDay] : ""
    }

    var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy年M月d日"
        return formatter.string(from: Date())
    }

    func timeString(for slotNumber: Int) -> String {
        timeSlots.first { $0.number == slotNumber }.map { "\($0.startTime)-\($0.endTime)" } ?? ""
    }
}
