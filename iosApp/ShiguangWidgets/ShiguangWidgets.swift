import SwiftUI
import WidgetKit

@main
struct ShiguangWidgetBundle: WidgetBundle {
    var body: some Widget {
        NextCourseWidget()
        TodayCoursesWidget()
        TwoDayWidget()
        CourseListWidget()
    }
}

struct NextCourseWidget: Widget {
    let kind = "NextCourseWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ScheduleProvider()) { NextCourseWidgetView(entry: $0) }
            .configurationDisplayName("下一节课")
            .description("查看今天即将开始的课程。")
            .supportedFamilies([.systemSmall])
    }
}

struct TodayCoursesWidget: Widget {
    let kind = "TodayCoursesWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ScheduleProvider()) { TodayCoursesWidgetView(entry: $0) }
            .configurationDisplayName("今日课程")
            .description("查看今天剩余的课程。")
            .supportedFamilies([.systemMedium])
    }
}

struct TwoDayWidget: Widget {
    let kind = "TwoDayWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ScheduleProvider()) { TwoDayWidgetView(entry: $0) }
            .configurationDisplayName("今明课程")
            .description("并排查看今天和明天的课程。")
            .supportedFamilies([.systemLarge])
    }
}

struct CourseListWidget: Widget {
    let kind = "CourseListWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ScheduleProvider()) { CourseListWidgetView(entry: $0) }
            .configurationDisplayName("课程列表")
            .description("按时间查看剩余课程。")
            .supportedFamilies([.systemMedium, .systemLarge])
    }
}