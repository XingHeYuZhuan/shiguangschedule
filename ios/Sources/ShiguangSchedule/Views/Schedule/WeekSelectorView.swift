import SwiftUI

struct WeekSelectorView: View {
    @Binding var currentWeek: Int
    let totalWeeks: Int
    let tables: [CourseTableModel]
    @Binding var currentTableId: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section("课表") {
                    ForEach(tables, id: \.id) { table in
                        Button {
                            currentTableId = table.id
                        } label: {
                            HStack {
                                Text(table.name)
                                    .foregroundColor(.primary)
                                Spacer()
                                if table.id == currentTableId {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.accentColor)
                                }
                            }
                        }
                    }
                }

                Section("选择周次") {
                    VStack {
                        Picker("当前周", selection: $currentWeek) {
                            ForEach(1...totalWeeks, id: \.self) { week in
                                Text("第\(week)周").tag(week)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(height: 160)

                        Text("第\(currentWeek)周 / 共\(totalWeeks)周")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("切换课表")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成") { dismiss() }
                }
            }
        }
    }
}
