import SwiftUI

struct CourseDetailSheet: View {
    let course: CourseModel
    var onEdit: (() -> Void)? = nil
    var onDelete: (() -> Void)? = nil
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        Circle()
                            .fill(Color(hex: ScheduleGridStyle.defaultColors[safe: course.colorInt]?.lightColor ?? 0))
                            .frame(width: 16, height: 16)
                        Text(course.name)
                            .font(.title2.bold())
                    }
                }

                Section("详情") {
                    if !course.teacher.isEmpty {
                        LabeledContent("教师", value: course.teacher)
                    }
                    if !course.position.isEmpty {
                        LabeledContent("地点", value: course.position)
                    }
                    LabeledContent("星期", value: dayName(course.day))
                    if let s = course.startSection, let e = course.endSection {
                        LabeledContent("节次", value: "第\(s)-\(e)节")
                    }
                    if !course.weekNumbers.isEmpty {
                        LabeledContent("周次", value: weekRangeString(course.weekNumbers))
                    }
                    if let remark = course.remark, !remark.isEmpty {
                        LabeledContent("备注", value: remark)
                    }
                }
            }
            .navigationTitle("课程详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
                ToolbarItem(placement: .bottomBar) {
                    HStack {
                        Button(role: .destructive) {
                            onDelete?()
                            dismiss()
                        } label: {
                            Label("删除", systemImage: "trash")
                        }
                        Spacer()
                        Button {
                            onEdit?()
                            dismiss()
                        } label: {
                            Label("编辑", systemImage: "pencil")
                        }
                    }
                }
            }
        }
    }

    private func dayName(_ day: Int) -> String {
        ["", "周一", "周二", "周三", "周四", "周五", "周六", "周日"][safe: day] ?? ""
    }

    private func weekRangeString(_ weeks: [Int]) -> String {
        let sorted = weeks.sorted()
        guard !sorted.isEmpty else { return "" }
        var result: [String] = []
        var start = sorted[0]
        var end = sorted[0]
        for w in sorted.dropFirst() {
            if w == end + 1 {
                end = w
            } else {
                result.append(start == end ? "\(start)" : "\(start)-\(end)")
                start = w
                end = w
            }
        }
        result.append(start == end ? "\(start)" : "\(start)-\(end)")
        return "第\(result.joined(separator: ","))周"
    }
}
