import Foundation

struct WeekCalculationService {
    static func currentWeekNumber(semesterStartDate: String?) -> Int {
        guard let startStr = semesterStartDate,
              let startDate = dateFromString(startStr) else {
            return 1
        }
        let today = Date()
        let calendar = Calendar.current
        let startWeek = calendar.component(.weekOfYear, from: startDate)
        let currentWeek = calendar.component(.weekOfYear, from: today)
        let diff = currentWeek - startWeek + 1
        return max(1, diff)
    }

    static func currentDayOfWeek(firstDayOfWeek: Int = 1) -> Int {
        let calendar = Calendar.current
        var weekday = calendar.component(.weekday, from: Date())
        // In Calendar.current, sunday=1, monday=2, ..., saturday=7
        // We want monday=1, ..., sunday=7
        weekday = weekday == 1 ? 7 : weekday - 1
        // Adjust based on firstDayOfWeek
        if firstDayOfWeek > 1 {
            weekday = ((weekday - firstDayOfWeek + 7) % 7) + 1
        }
        return weekday
    }

    static func weekDateRange(semesterStartDate: String?, weekNumber: Int) -> (start: Date, end: Date)? {
        guard let startStr = semesterStartDate,
              let startDate = dateFromString(startStr) else {
            return nil
        }
        let calendar = Calendar.current
        guard let weekStart = calendar.date(byAdding: .day, value: (weekNumber - 1) * 7, to: startDate),
              let weekEnd = calendar.date(byAdding: .day, value: 6, to: weekStart) else {
            return nil
        }
        return (weekStart, weekEnd)
    }

    static func dateFromString(_ dateStr: String) -> Date? {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: dateStr)
    }

    static func stringFromDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    static func isCourseInWeek(course: CourseModel, weekNumber: Int) -> Bool {
        course.weeks.contains { $0.weekNumber == weekNumber }
    }

    static func isTodayHoliday(holidays: [String: Bool], date: Date) -> Bool {
        let dateStr = stringFromDate(date)
        return holidays[dateStr] == true
    }

    static func weekLabel(weekNumber: Int, totalWeeks: Int) -> String {
        "第\(weekNumber)周 / 共\(totalWeeks)周"
    }
}
