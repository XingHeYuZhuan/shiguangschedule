import Foundation
import SwiftData

@MainActor
class DatabaseService {
    static let shared = DatabaseService()

    var container: ModelContainer = {
        let schema = Schema([
            CourseTableModel.self,
            CourseModel.self,
            CourseWeekModel.self,
            TimeSlotModel.self,
            CourseTableConfigModel.self,
        ])
        let config = ModelConfiguration(isStoredInMemoryOnly: false)
        do {
            return try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()

    var context: ModelContext {
        container.mainContext
    }

    private init() {}

    func setupDefaultDataIfNeeded() {
        let descriptor = FetchDescriptor<CourseTableModel>()
        guard (try? context.fetch(descriptor))?.isEmpty ?? true else { return }

        let defaultTable = CourseTableModel(name: "我的课表")
        context.insert(defaultTable)

        let config = CourseTableConfigModel(
            courseTable: defaultTable,
            showWeekends: false,
            semesterTotalWeeks: 20,
            defaultClassDuration: 45,
            defaultBreakDuration: 10,
            firstDayOfWeek: 1
        )
        context.insert(config)
        defaultTable.config = config

        let defaultTimeSlots: [(Int, String, String, String?)] = [
            (1, "08:00", "08:45", nil),
            (2, "08:50", "09:35", nil),
            (3, "09:50", "10:35", nil),
            (4, "10:40", "11:25", nil),
            (5, "11:30", "12:15", nil),
            (6, "13:30", "14:15", nil),
            (7, "14:20", "15:05", nil),
            (8, "15:20", "16:05", nil),
            (9, "16:10", "16:55", nil),
            (10, "17:00", "17:45", nil),
            (11, "18:30", "19:15", nil),
            (12, "19:20", "20:05", nil),
            (13, "20:10", "20:55", nil),
        ]

        for (num, start, end, alias) in defaultTimeSlots {
            let slot = TimeSlotModel(
                number: num,
                startTime: start,
                endTime: end,
                courseTable: defaultTable,
                alias: alias
            )
            context.insert(slot)
        }

        if AppSettings.shared.currentCourseTableId.isEmpty {
            AppSettings.shared.currentCourseTableId = defaultTable.id
        }

        try? context.save()
    }

    // MARK: - CourseTable Operations

    func getAllCourseTables() -> [CourseTableModel] {
        let descriptor = FetchDescriptor<CourseTableModel>(sortBy: [SortDescriptor(\.createdAt)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func getCourseTable(by id: String) -> CourseTableModel? {
        let descriptor = FetchDescriptor<CourseTableModel>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func getCurrentCourseTable() -> CourseTableModel? {
        let id = AppSettings.shared.currentCourseTableId
        return getCourseTable(by: id) ?? getAllCourseTables().first
    }

    func deleteCourseTable(_ table: CourseTableModel) {
        context.delete(table)
        try? context.save()
    }

    // MARK: - Course Operations

    func getCourses(for courseTableId: String) -> [CourseModel] {
        let descriptor = FetchDescriptor<CourseModel>(predicate: #Predicate { $0.courseTable?.id == courseTableId })
        return (try? context.fetch(descriptor)) ?? []
    }

    func getCourses(by day: Int, courseTableId: String) -> [CourseModel] {
        let descriptor = FetchDescriptor<CourseModel>(
            predicate: #Predicate { $0.courseTable?.id == courseTableId && $0.day == day }
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func getCoursesForWeek(weekNumber: Int, courseTableId: String) -> [CourseModel] {
        let allCourses = getCourses(for: courseTableId)
        return allCourses.filter { course in
            course.weeks.contains { $0.weekNumber == weekNumber }
        }
    }

    func deleteCourse(_ course: CourseModel) {
        context.delete(course)
        try? context.save()
    }

    // MARK: - TimeSlot Operations

    func getTimeSlots(for courseTableId: String) -> [TimeSlotModel] {
        let descriptor = FetchDescriptor<TimeSlotModel>(
            predicate: #Predicate { $0.courseTable?.id == courseTableId },
            sortBy: [SortDescriptor(\.number)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func replaceAllTimeSlots(_ slots: [TimeSlotModel], for courseTableId: String) {
        guard let table = getCourseTable(by: courseTableId) else { return }
        let existing = getTimeSlots(for: courseTableId)
        for slot in existing { context.delete(slot) }
        for slot in slots {
            slot.courseTable = table
            context.insert(slot)
        }
        try? context.save()
    }

    // MARK: - Config Operations

    func getConfig(for courseTableId: String) -> CourseTableConfigModel? {
        let descriptor = FetchDescriptor<CourseTableConfigModel>(
            predicate: #Predicate { $0.courseTable?.id == courseTableId }
        )
        return try? context.fetch(descriptor).first
    }

    func saveConfig(_ config: CourseTableConfigModel) {
        try? context.save()
    }
}
