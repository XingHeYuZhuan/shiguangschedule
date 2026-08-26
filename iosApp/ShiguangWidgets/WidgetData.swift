import Foundation
import SwiftUI
import WidgetKit

let shiguangAppGroup = "group.com.xingheyuzhuan.shiguangschedule"
let shiguangSnapshotKey = "ios_widget_snapshot"

struct CourseSnapshot: Codable, Identifiable {
    let id: String
    let name: String
    let teacher: String
    let position: String
    let startTime: String
    let endTime: String
    let isSkipped: Bool
    let date: String
    let colorIndex: Int
}

struct CourseColorSnapshot: Codable {
    let lightArgb: String
    let darkArgb: String
}

struct ScheduleSnapshot: Codable {
    let generatedAtEpochMillis: Int64
    let semesterStartDate: String?
    let semesterTotalWeeks: Int
    let firstDayOfWeek: Int
    let courses: [CourseSnapshot]
    let colors: [CourseColorSnapshot]

    static let empty = ScheduleSnapshot(
        generatedAtEpochMillis: 0,
        semesterStartDate: nil,
        semesterTotalWeeks: 0,
        firstDayOfWeek: 1,
        courses: [],
        colors: []
    )

    func currentWeek(on date: Date, calendar: Calendar = .current) -> Int? {
        guard let semesterStartDate,
              semesterTotalWeeks > 0,
              let start = WidgetDateParser.date(semesterStartDate, calendar: calendar) else { return nil }

        var scheduleCalendar = calendar
        scheduleCalendar.firstWeekday = firstDayOfWeek == 7 ? 1 : firstDayOfWeek + 1
        let alignedStart = scheduleCalendar.dateInterval(of: .weekOfYear, for: start)?.start ?? start
        let alignedDate = scheduleCalendar.dateInterval(of: .weekOfYear, for: date)?.start ?? date
        guard alignedDate >= alignedStart else { return nil }
        let week = (calendar.dateComponents([.day], from: alignedStart, to: alignedDate).day ?? 0) / 7 + 1
        return (1...semesterTotalWeeks).contains(week) ? week : nil
    }

    func courses(on date: Date, calendar: Calendar = .current) -> [CourseSnapshot] {
        let dateString = WidgetDateParser.dateString(date, calendar: calendar)
        return courses
            .filter { $0.date == dateString && !$0.isSkipped }
            .sorted { $0.startTime < $1.startTime }
    }

    func remainingCourses(on date: Date, calendar: Calendar = .current) -> [CourseSnapshot] {
        courses(on: date, calendar: calendar).filter {
            guard let end = WidgetDateParser.date($0.date, time: $0.endTime, calendar: calendar) else { return true }
            return end > date
        }
    }

    func color(for course: CourseSnapshot, colorScheme: ColorScheme) -> Color {
        guard colors.indices.contains(course.colorIndex) else { return .accentColor }
        let value = colorScheme == .dark ? colors[course.colorIndex].darkArgb : colors[course.colorIndex].lightArgb
        return Color(argbHex: value) ?? .accentColor
    }
}

enum WidgetDateParser {
    static func date(_ value: String, calendar: Calendar = .current) -> Date? {
        let parts = value.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }

    static func date(_ value: String, time: String, calendar: Calendar = .current) -> Date? {
        let dateParts = value.split(separator: "-").compactMap { Int($0) }
        let timeParts = time.split(separator: ":").compactMap { Int($0) }
        guard dateParts.count == 3, timeParts.count >= 2 else { return nil }
        return calendar.date(from: DateComponents(
            year: dateParts[0], month: dateParts[1], day: dateParts[2],
            hour: timeParts[0], minute: timeParts[1]
        ))
    }

    static func dateString(_ date: Date, calendar: Calendar = .current) -> String {
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", components.year ?? 0, components.month ?? 0, components.day ?? 0)
    }
}

extension Color {
    init?(argbHex: String) {
        guard let value = UInt64(argbHex, radix: 16) else { return nil }
        self.init(
            .sRGB,
            red: Double((value >> 16) & 0xff) / 255,
            green: Double((value >> 8) & 0xff) / 255,
            blue: Double(value & 0xff) / 255,
            opacity: Double((value >> 24) & 0xff) / 255
        )
    }
}

struct ScheduleEntry: TimelineEntry {
    let date: Date
    let snapshot: ScheduleSnapshot
}

struct ScheduleProvider: TimelineProvider {
    func placeholder(in context: Context) -> ScheduleEntry {
        ScheduleEntry(date: .now, snapshot: .preview)
    }

    func getSnapshot(in context: Context, completion: @escaping (ScheduleEntry) -> Void) {
        completion(ScheduleEntry(date: .now, snapshot: context.isPreview ? .preview : loadSnapshot()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ScheduleEntry>) -> Void) {
        let now = Date()
        let snapshot = loadSnapshot()
        let calendar = Calendar.current
        let tomorrow = calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: now)) ?? now.addingTimeInterval(6 * 3600)
        let courseChanges = snapshot.courses.flatMap { course in
            [
                WidgetDateParser.date(course.date, time: course.startTime, calendar: calendar),
                WidgetDateParser.date(course.date, time: course.endTime, calendar: calendar)
            ].compactMap { $0 }
        }
        let dates = ([now, tomorrow] + courseChanges)
            .filter { $0 >= now && $0 <= now.addingTimeInterval(24 * 3600) }
            .sorted()
        let entries = dates.map { ScheduleEntry(date: $0, snapshot: snapshot) }
        completion(Timeline(entries: entries, policy: .after(now.addingTimeInterval(6 * 3600))))
    }

    private func loadSnapshot() -> ScheduleSnapshot {
        guard let defaults = UserDefaults(suiteName: shiguangAppGroup),
              let json = defaults.string(forKey: shiguangSnapshotKey),
              let data = json.data(using: .utf8),
              let snapshot = try? JSONDecoder().decode(ScheduleSnapshot.self, from: data) else { return .empty }
        return snapshot
    }
}

extension ScheduleSnapshot {
    static let preview = ScheduleSnapshot(
        generatedAtEpochMillis: 0,
        semesterStartDate: WidgetDateParser.dateString(Calendar.current.date(byAdding: .day, value: -21, to: .now) ?? .now),
        semesterTotalWeeks: 20,
        firstDayOfWeek: 1,
        courses: [
            CourseSnapshot(id: "1", name: "高等数学", teacher: "张老师", position: "教学楼 A203", startTime: "08:30", endTime: "10:05", isSkipped: false, date: WidgetDateParser.dateString(.now), colorIndex: 0),
            CourseSnapshot(id: "2", name: "计算机网络", teacher: "李老师", position: "实验楼 402", startTime: "14:00", endTime: "23:59", isSkipped: false, date: WidgetDateParser.dateString(.now), colorIndex: 1)
        ],
        colors: [
            CourseColorSnapshot(lightArgb: "ffffcc99", darkArgb: "ff663300"),
            CourseColorSnapshot(lightArgb: "ff99e6ff", darkArgb: "ff004d66")
        ]
    )
}