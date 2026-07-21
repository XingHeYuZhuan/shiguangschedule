import SwiftUI

struct CourseManagementView: View {
    @State private var vm = CourseViewModel()
    @State private var showAddCourse = false
    @State private var showDetail: CourseModel? = nil
    @State private var showDeleteAlert = false
    @State private var courseToDelete: CourseModel? = nil

    var body: some View {
        List {
            ForEach(vm.coursesGroupedByName(), id: \.name) { group in
                Section(group.name) {
                    ForEach(group.courses, id: \.id) { course in
                        Button {
                            showDetail = course
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                HStack {
                                    Circle()
                                        .fill(Color(hex: ScheduleGridStyle.defaultColors[safe: course.colorInt]?.lightColor ?? 0))
                                        .frame(width: 10, height: 10)
                                    Text("\(vm.dayNames[safe: course.day] ?? "") 第\(course.startSection ?? 0)-\(course.endSection ?? 0)节")
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }
                                if !course.teacher.isEmpty {
                                    Text(course.teacher)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                if !course.position.isEmpty {
                                    Text(course.position)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                        .contextMenu {
                            Button("编辑") { showDetail = course }
                            Button("复制") { vm.duplicateCourse(course) }
                            Button("删除", role: .destructive) {
                                courseToDelete = course
                                showDeleteAlert = true
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("课程管理")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showAddCourse = true } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .onAppear {
            let tableId = AppSettings.shared.currentCourseTableId
            vm.load(for: tableId)
        }
        .sheet(isPresented: $showAddCourse) {
            AddEditCourseView(courseTableId: vm.courseTableId) {
                vm.load(for: vm.courseTableId)
            }
        }
        .sheet(item: $showDetail) { course in
            AddEditCourseView(
                courseTableId: vm.courseTableId,
                existingCourse: course,
                onSave: { vm.load(for: vm.courseTableId) }
            )
        }
        .alert("确认删除", isPresented: $showDeleteAlert) {
            Button("取消", role: .cancel) {}
            Button("删除", role: .destructive) {
                if let course = courseToDelete {
                    vm.deleteCourse(course)
                }
            }
        } message: {
            Text("确定要删除这门课程吗？此操作不可撤销。")
        }
    }
}

extension CourseModel: Identifiable {}
