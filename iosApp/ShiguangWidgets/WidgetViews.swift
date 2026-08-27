import SwiftUI
import WidgetKit

private let appURL = URL(string: "shiguangschedule://schedule")!

struct WidgetSurface<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        if #available(iOS 17.0, *) {
            content
                .containerBackground(for: .widget) { Color(.systemBackground) }
                .widgetURL(appURL)
        } else {
            content
                .background(Color(.systemBackground))
                .widgetURL(appURL)
        }
    }
}

struct CourseRow: View {
    let course: CourseSnapshot
    let snapshot: ScheduleSnapshot
    var compact = false
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 8) {
            RoundedRectangle(cornerRadius: 2)
                .fill(snapshot.color(for: course, colorScheme: colorScheme))
                .frame(width: 4)
            VStack(alignment: .leading, spacing: compact ? 1 : 3) {
                Text(course.name).font(.system(size: compact ? 12 : 13, weight: .semibold)).lineLimit(1)
                HStack(spacing: 6) {
                    Label(timeRange, systemImage: "clock")
                    if !course.position.isEmpty { Label(course.position, systemImage: "mappin") }
                }
                .font(.system(size: compact ? 9 : 10))
                .foregroundStyle(.secondary)
                .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
    }

    private var timeRange: String {
        "\(course.startTime.prefix(5))-\(course.endTime.prefix(5))"
    }
}

struct EmptyScheduleView: View {
    let title: String
    var subtitle: String? = nil

    var body: some View {
        VStack(spacing: 5) {
            Image(systemName: "calendar.badge.checkmark")
                .font(.title3)
                .foregroundStyle(.secondary)
            Text(title).font(.system(size: 14, weight: .semibold))
            if let subtitle { Text(subtitle).font(.caption2).foregroundStyle(.secondary) }
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct NextCourseWidgetView: View {
    let entry: ScheduleEntry
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        WidgetSurface {
            if entry.snapshot.currentWeek(on: entry.date) == nil {
                EmptyScheduleView(title: "假期中", subtitle: "期待新学期")
            } else if let course = entry.snapshot.remainingCourses(on: entry.date).first {
                VStack(alignment: .leading, spacing: 7) {
                    HStack {
                        Text("下一节").font(.caption2).foregroundStyle(.secondary)
                        Spacer()
                        Text("\(entry.snapshot.remainingCourses(on: entry.date).count)")
                            .font(.caption.bold()).monospacedDigit()
                            .padding(5)
                            .background(entry.snapshot.color(for: course, colorScheme: colorScheme).opacity(0.45), in: Circle())
                    }
                    Text(course.name).font(.system(size: 15, weight: .bold)).lineLimit(2)
                    Spacer(minLength: 0)
                    Label("\(course.startTime.prefix(5)) - \(course.endTime.prefix(5))", systemImage: "clock")
                    Label(course.position.isEmpty ? "未设置地点" : course.position, systemImage: "mappin")
                }
                .font(.caption)
            } else {
                EmptyScheduleView(title: entry.snapshot.courses(on: entry.date).isEmpty ? "今日无课" : "今日课程已结束")
            }
        }
    }
}

struct TodayCoursesWidgetView: View {
    let entry: ScheduleEntry

    var body: some View {
        WidgetSurface {
            VStack(alignment: .leading, spacing: 8) {
                WidgetHeader(title: weekday(entry.date), week: entry.snapshot.currentWeek(on: entry.date), count: courses.count)
                if courses.isEmpty {
                    EmptyScheduleView(title: entry.snapshot.courses(on: entry.date).isEmpty ? "今日无课" : "今日课程已结束")
                } else {
                    ForEach(courses.prefix(3)) { CourseRow(course: $0, snapshot: entry.snapshot, compact: true) }
                }
            }
        }
    }

    private var courses: [CourseSnapshot] { entry.snapshot.remainingCourses(on: entry.date) }
}

struct TwoDayWidgetView: View {
    let entry: ScheduleEntry

    var body: some View {
        WidgetSurface {
            if entry.snapshot.currentWeek(on: entry.date) == nil {
                EmptyScheduleView(title: "假期中", subtitle: "期待新学期")
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    WidgetHeader(title: "今明课程", week: entry.snapshot.currentWeek(on: entry.date), count: nil)
                    HStack(alignment: .top, spacing: 12) {
                        DayColumn(title: "今天", courses: entry.snapshot.remainingCourses(on: entry.date), snapshot: entry.snapshot)
                        Divider()
                        DayColumn(title: "明天", courses: tomorrowCourses, snapshot: entry.snapshot)
                    }
                }
            }
        }
    }

    private var tomorrowCourses: [CourseSnapshot] {
        entry.snapshot.courses(on: Calendar.current.date(byAdding: .day, value: 1, to: entry.date) ?? entry.date)
    }
}

struct CourseListWidgetView: View {
    let entry: ScheduleEntry
    @Environment(\.widgetFamily) private var widgetFamily

    var body: some View {
        WidgetSurface {
            VStack(alignment: .leading, spacing: 9) {
                WidgetHeader(title: "课程列表", week: entry.snapshot.currentWeek(on: entry.date), count: courses.count)
                if courses.isEmpty {
                    EmptyScheduleView(title: entry.snapshot.courses(on: entry.date).isEmpty ? "今日无课" : "今日课程已结束")
                } else {
                    ForEach(courses.prefix(rowLimit)) { CourseRow(course: $0, snapshot: entry.snapshot) }
                }
            }
        }
    }

    private var courses: [CourseSnapshot] { entry.snapshot.remainingCourses(on: entry.date) }
    private var rowLimit: Int { widgetFamily == .systemMedium ? 3 : 6 }
}

struct WidgetHeader: View {
    let title: String
    let week: Int?
    let count: Int?

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title).font(.system(size: 13, weight: .bold))
            if let count { Text("剩余 \(count) 节").font(.caption2).foregroundStyle(.secondary) }
            Spacer()
            if let week { Text("第 \(week) 周").font(.caption2).foregroundStyle(.secondary) }
        }
    }
}

struct DayColumn: View {
    let title: String
    let courses: [CourseSnapshot]
    let snapshot: ScheduleSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack {
                Text(title).font(.caption.bold())
                Spacer()
                Text("\(courses.count) 节").font(.caption2).foregroundStyle(.secondary)
            }
            if courses.isEmpty {
                Text("无课程").font(.caption).foregroundStyle(.secondary).frame(maxWidth: .infinity, minHeight: 60)
            } else {
                ForEach(courses.prefix(4)) { CourseRow(course: $0, snapshot: snapshot, compact: true) }
            }
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }
}

private func weekday(_ date: Date) -> String {
    date.formatted(.dateTime.weekday(.wide))
}