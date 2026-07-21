import SwiftUI

struct AddEditCourseView: View {
    let courseTableId: String
    var existingCourse: CourseModel? = nil
    var onSave: (() -> Void)? = nil

    @State private var vm = CourseViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                basicInfoSection
                timeSection
                weeksSection
                colorSection
                remarkSection
            }
            .navigationTitle(existingCourse != nil ? "编辑课程" : "添加课程")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        if vm.save() {
                            onSave?()
                            dismiss()
                        }
                    }
                    .disabled(vm.name.trimmingCharacters(in: .whitespaces).isEmpty || vm.selectedWeeks.isEmpty)
                }
            }
            .onAppear {
                vm.load(for: courseTableId)
                vm.loadCourse(existingCourse)
            }
        }
    }

    private var basicInfoSection: some View {
        Section("基本信息") {
            TextField("课程名称", text: $vm.name)
            TextField("授课教师", text: $vm.teacher)
            TextField("上课地点", text: $vm.position)

            Picker("星期", selection: $vm.day) {
                ForEach(1...7, id: \.self) { day in
                    Text(vm.dayNames[safe: day] ?? "周\(day)").tag(day)
                }
            }
        }
    }

    private var timeSection: some View {
        Section("时间") {
            Toggle("自定义时间", isOn: $vm.isCustomTime)

            if vm.isCustomTime {
                HStack {
                    Text("开始")
                    Spacer()
                    TextField("HH:MM", text: $vm.customStartTime)
                        .keyboardType(.numbersAndPunctuation)
                        .multilineTextAlignment(.trailing)
                }
                HStack {
                    Text("结束")
                    Spacer()
                    TextField("HH:MM", text: $vm.customEndTime)
                        .keyboardType(.numbersAndPunctuation)
                        .multilineTextAlignment(.trailing)
                }
            } else {
                Picker("开始节次", selection: $vm.startSection) {
                    ForEach(1...13, id: \.self) { n in
                        Text("第\(n)节").tag(n)
                    }
                }
                Picker("结束节次", selection: $vm.endSection) {
                    ForEach(vm.startSection...13, id: \.self) { n in
                        Text("第\(n)节").tag(n)
                    }
                }
            }
        }
    }

    private var weeksSection: some View {
        Section {
            HStack {
                Button("全选") { vm.selectAllWeeks() }
                    .buttonStyle(.bordered)
                Button("清除") { vm.clearWeeks() }
                    .buttonStyle(.bordered)
                Button("奇数周") { vm.selectOddWeeks() }
                    .buttonStyle(.bordered)
                Button("偶数周") { vm.selectEvenWeeks() }
                    .buttonStyle(.bordered)
            }
            .buttonStyle(.borderedTinted)
            .font(.caption)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7), spacing: 4) {
                ForEach(1...vm.totalWeeks, id: \.self) { week in
                    Text("\(week)")
                        .font(.system(size: 12, weight: vm.selectedWeeks.contains(week) ? .bold : .regular))
                        .frame(width: 32, height: 32)
                        .background(vm.selectedWeeks.contains(week) ? Color.accentColor : Color(.systemGray6))
                        .foregroundColor(vm.selectedWeeks.contains(week) ? .white : .primary)
                        .clipShape(Circle())
                        .onTapGesture { vm.toggleWeek(week) }
                }
            }
            .padding(.vertical, 4)

            Text("已选 \(vm.selectedWeeks.count) 周")
                .font(.caption)
                .foregroundColor(.secondary)
        } header: {
            Text("上课周次")
        }
    }

    private var colorSection: some View {
        Section("颜色") {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 6), spacing: 8) {
                ForEach(ScheduleGridStyle.defaultColors.indices, id: \.self) { index in
                    let color = Color(hex: ScheduleGridStyle.defaultColors[index].lightColor)
                    Circle()
                        .fill(color)
                        .frame(width: 36, height: 36)
                        .overlay(
                            vm.colorInt == index ?
                            Image(systemName: "checkmark")
                                .font(.caption.bold())
                                .foregroundColor(.white) : nil
                        )
                        .onTapGesture { vm.colorInt = index }
                }
            }
            .padding(.vertical, 4)
        }
    }

    private var remarkSection: some View {
        Section("备注") {
            TextField("备注（限300字）", text: $vm.remark)
        }
    }
}
