import SwiftUI

struct TodayScheduleView: View {
    @State private var vm = TodayViewModel()
    @State private var showAddCourse = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Date & Day header
                VStack(spacing: 4) {
                    Text(vm.formattedDate)
                        .font(.headline)
                        .foregroundColor(.secondary)
                    Text(vm.todayDayName)
                        .font(.largeTitle.bold())
                    Text("第\(vm.currentWeek)周")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top)

                if vm.isHoliday {
                    VStack(spacing: 8) {
                        Image(systemName: "leaf.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.green)
                        Text("今日放假")
                            .font(.title2.bold())
                        Text("好好休息吧！")
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 40)
                } else if vm.todayCourses.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: "checkmark.circle")
                            .font(.system(size: 40))
                            .foregroundColor(.green)
                        Text("今日无课")
                            .font(.title2.bold())
                        Text("享受自由时光！")
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 40)
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(vm.todayCourses, id: \.id) { course in
                            TodayCourseCard(
                                course: course,
                                timeRange: vm.timeString(for: course.startSection ?? 0)
                            )
                        }
                    }
                    .padding(.horizontal)
                }
            }
        }
        .navigationTitle("今日课程")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showAddCourse = true } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .onAppear { vm.load() }
        .refreshable { vm.load() }
        .sheet(isPresented: $showAddCourse) {
            AddEditCourseView(
                courseTableId: vm.currentTable?.id ?? "",
                onSave: { vm.load() }
            )
        }
    }
}

struct TodayCourseCard: View {
    let course: CourseModel
    let timeRange: String

    var body: some View {
        HStack(spacing: 16) {
            RoundedRectangle(cornerRadius: 4)
                .fill(Color(hex: ScheduleGridStyle.defaultColors[safe: course.colorInt]?.lightColor ?? 0))
                .frame(width: 4)

            VStack(alignment: .leading, spacing: 4) {
                Text(course.name)
                    .font(.headline)
                if !course.teacher.isEmpty {
                    Label(course.teacher, systemImage: "person")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                if !course.position.isEmpty {
                    Label(course.position, systemImage: "location")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                if let s = course.startSection, let e = course.endSection {
                    Text("第\(s)-\(e)节")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            if !timeRange.isEmpty {
                Text(timeRange)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}
