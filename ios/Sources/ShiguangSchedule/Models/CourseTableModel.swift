import Foundation
import SwiftData

@Model
final class CourseTableModel {
    @Attribute(.unique) var id: String
    var name: String
    var createdAt: Date

    @Relationship(deleteRule: .cascade, inverse: \CourseModel.courseTable)
    var courses: [CourseModel] = []

    @Relationship(deleteRule: .cascade, inverse: \TimeSlotModel.courseTable)
    var timeSlots: [TimeSlotModel] = []

    @Relationship(deleteRule: .cascade)
    var config: CourseTableConfigModel?

    init(id: String = UUID().uuidString, name: String, createdAt: Date = Date()) {
        self.id = id
        self.name = name
        self.createdAt = createdAt
    }
}
