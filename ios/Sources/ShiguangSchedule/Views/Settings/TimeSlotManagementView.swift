import SwiftUI

struct TimeSlotManagementView: View {
    @State private var vm = SettingsViewModel()
    @State private var slots: [(number: Int, start: String, end: String, alias: String?)] = []
    @State private var showAddSlot = false

    var body: some View {
        List {
            ForEach(slots.indices, id: \.self) { index in
                HStack {
                    Text("第\(slots[index].number)节")
                        .font(.subheadline.bold())
                        .frame(width: 50)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(slots[index].start) - \(slots[index].end)")
                            .font(.subheadline)
                        if let alias = slots[index].alias, !alias.isEmpty {
                            Text(alias)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    Spacer()
                    Button("编辑") { editSlot(at: index) }
                        .font(.caption)
                }
            }
            .onDelete { indices in
                slots.remove(atOffsets: indices)
                renumberSlots()
                vm.saveTimeSlots(slots)
            }
        }
        .navigationTitle("节次管理")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showAddSlot = true } label: {
                    Image(systemName: "plus")
                }
            }
            ToolbarItem(placement: .topBarLeading) {
                EditButton()
            }
        }
        .onAppear {
            vm.load()
            slots = vm.timeSlots.map { ($0.number, $0.startTime, $0.endTime, $0.alias) }
        }
        .sheet(isPresented: $showAddSlot) {
            addSlotSheet
        }
    }

    private var addSlotSheet: some View {
        NavigationStack {
            Form {
                let nextNum = (slots.last?.number ?? 0) + 1
                Text("将添加第\(nextNum)节")
                    .font(.headline)
                HStack {
                    Text("开始")
                    Spacer()
                    TextField("HH:MM", text: Binding(
                        get: { "08:00" },
                        set: { _ in }
                    ))
                    .keyboardType(.numbersAndPunctuation)
                }
                HStack {
                    Text("结束")
                    Spacer()
                    TextField("HH:MM", text: Binding(
                        get: { "08:45" },
                        set: { _ in }
                    ))
                    .keyboardType(.numbersAndPunctuation)
                }
            }
            .navigationTitle("添加节次")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("添加") {
                        let num = (slots.last?.number ?? 0) + 1
                        slots.append((num, "08:00", "08:45", nil))
                        vm.saveTimeSlots(slots)
                        showAddSlot = false
                    }
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { showAddSlot = false }
                }
            }
        }
    }

    private func editSlot(at index: Int) {
        // Simple inline editing via alert
    }

    private func renumberSlots() {
        for i in slots.indices {
            slots[i].number = i + 1
        }
    }
}
