import SwiftUI
import SwiftData

struct WeeklyScheduleView: View {
    @State private var vm = ScheduleViewModel()
    @State private var showWeekPicker = false
    @State private var showAddCourse = false
    @State private var selectedCourse: CourseModel? = nil

    private let slotHeight: CGFloat = 56
    private let timeColWidth: CGFloat = 44

    var body: some View {
        VStack(spacing: 0) {
            weekHeader
            dayHeaders
            scrollGrid
        }
        .navigationTitle(vm.weekLabel)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(vm.currentTable?.name ?? "课表") {
                    showWeekPicker = true
                }
                .font(.headline)
            }
            ToolbarItem(placement: .topBarTrailing) {
                HStack(spacing: 12) {
                    Button { vm.previousWeek() } label: {
                        Image(systemName: "chevron.left")
                    }
                    Button { vm.currentWeek = WeekCalculationService.currentWeekNumber(semesterStartDate: vm.semesterStartDate) } label: {
                        Image(systemName: "location.fill")
                            .font(.caption)
                    }
                    Button { vm.nextWeek() } label: {
                        Image(systemName: "chevron.right")
                    }
                    Button { showAddCourse = true } label: {
                        Image(systemName: "plus")
                    }
                }
            }
        }
        .onAppear { vm.loadCurrentTable() }
        .sheet(isPresented: $showWeekPicker) {
            WeekSelectorView(
                currentWeek: $vm.currentWeek,
                totalWeeks: vm.totalWeeks,
                tables: DatabaseService.shared.getAllCourseTables(),
                currentTableId: Binding(
                    get: { vm.currentTable?.id ?? "" },
                    set: { AppSettings.shared.currentCourseTableId = $0; vm.loadCurrentTable() }
                )
            )
        }
        .sheet(isPresented: $showAddCourse) {
            AddEditCourseView(courseTableId: vm.currentTable?.id ?? "", onSave: { vm.refresh() })
        }
        .sheet(item: $selectedCourse) { course in
            CourseDetailSheet(
                course: course,
                onEdit: {
                    selectedCourse = nil
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { showAddCourse = true }
                },
                onDelete: {
                    DatabaseService.shared.deleteCourse(course)
                    vm.refresh()
                    selectedCourse = nil
                }
            )
        }
        .refreshable { vm.refresh() }
    }

    private var weekHeader: some View {
        Text("第\(vm.currentWeek)周")
            .font(.title3.bold())
            .padding(.vertical, 8)
    }

    private var dayHeaders: some View {
        let days = vm.showWeekends
            ? ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]
            : ["周一", "周二", "周三", "周四", "周五"]

        return HStack(spacing: 2) {
            Text("")
                .frame(width: timeColWidth)
            ForEach(days, id: \.self) { day in
                Text(day)
                    .font(.caption2.bold())
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 4)
        .padding(.bottom, 4)
    }

    private var scrollGrid: some View {
        let slots = vm.timeSlots
        let weekCourses = vm.coursesForWeek(vm.currentWeek)
        let displayDays = vm.showWeekends ? Array(1...7) : Array(1...5)
        let totalHeight = CGFloat(slots.count) * slotHeight
        let minDayWidth: CGFloat = 80
        let dayWidth = max(minDayWidth, (UIScreen.main.bounds.width - timeColWidth - 16) / max(1, CGFloat(displayDays.count)))
        let totalWidth = timeColWidth + dayWidth * CGFloat(displayDays.count) + 8

        return ScrollView([.horizontal, .vertical]) {
            ZStack(alignment: .topLeading) {
                // Grid background
                VStack(spacing: 0) {
                    ForEach(slots, id: \.number) { _ in
                        Rectangle()
                            .fill(Color(.systemGray6))
                            .frame(height: slotHeight)
                            .overlay(Divider(), alignment: .bottom)
                    }
                }
                .frame(width: totalWidth)

                // Vertical grid lines (day separators)
                HStack(spacing: 0) {
                    Rectangle().fill(Color(.systemGray5)).frame(width: 0.5)
                    ForEach(0...displayDays.count, id: \.self) { _ in
                        Rectangle().fill(Color(.systemGray5)).frame(width: 0.5)
                            .frame(maxWidth: .infinity)
                    }
                }
                .frame(width: totalWidth)

                // Time labels
                VStack(spacing: 0) {
                    ForEach(slots, id: \.number) { slot in
                        VStack(spacing: 0) {
                            Text("\(slot.number)")
                                .font(.system(size: 11))
                            Text(slot.startTime.prefix(5))
                                .font(.system(size: 8))
                        }
                        .frame(width: timeColWidth, height: slotHeight)
                        .background(Color(.systemBackground))
                    }
                }

                // Course blocks
                ForEach(weekCourses, id: \.id) { course in
                    let dayIndex = displayDays.firstIndex(of: course.day) ?? 0
                    let startSlotIndex = slots.firstIndex { $0.number == course.startSection } ?? 0
                    let endSlotIndex = slots.firstIndex { $0.number == course.endSection } ?? startSlotIndex
                    let spanCount = max(1, endSlotIndex - startSlotIndex + 1)

                    let xOffset = timeColWidth + CGFloat(dayIndex) * dayWidth + 2
                    let yOffset = CGFloat(startSlotIndex) * slotHeight + 2
                    let blockWidth = max(0, dayWidth - 4)
                    let blockHeight = slotHeight * CGFloat(spanCount) - 4

                    courseBlock(course)
                        .frame(width: blockWidth, height: blockHeight)
                        .position(x: xOffset + blockWidth / 2, y: yOffset + blockHeight / 2)
                        .onTapGesture { selectedCourse = course }
                }
            }
            .frame(width: totalWidth, height: totalHeight)
        }
        .padding(.horizontal, 4)
    }

    private func courseBlock(_ course: CourseModel) -> some View {
        let colors = ScheduleGridStyle.defaultColors
        let colorIndex = min(course.colorInt, colors.count - 1)
        let colorPair = colors[safe: colorIndex] ?? colors[0]

        return RoundedRectangle(cornerRadius: 6)
            .fill(Color(hex: colorPair.lightColor).opacity(0.85))
            .overlay(
                VStack(spacing: 2) {
                    Text(course.name)
                        .font(.system(size: 11, weight: .medium))
                        .multilineTextAlignment(.center)
                        .lineLimit(3)
                    if !course.position.isEmpty {
                        Text(course.position)
                            .font(.system(size: 8))
                            .lineLimit(1)
                    }
                }
                .padding(4)
                .foregroundColor(.white),
                alignment: .top
            )
    }
}
