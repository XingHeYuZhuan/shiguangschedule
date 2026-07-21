import SwiftUI

struct CourseTableManagementView: View {
    @State private var vm = CourseTableManagerViewModel()
    @State private var showCreateAlert = false
    @State private var newTableName = ""
    @State private var showDeleteAlert = false
    @State private var tableToDelete: CourseTableModel? = nil
    @State private var showRenameAlert = false
    @State private var renameText = ""

    var body: some View {
        List {
            ForEach(vm.tables, id: \.id) { table in
                HStack {
                    Button {
                        vm.switchToTable(table.id)
                    } label: {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(table.name)
                                    .foregroundColor(.primary)
                                Text("\(DatabaseService.shared.getCourses(for: table.id).count) 门课程")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            if table.id == vm.currentTableId {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
                .swipeActions(edge: .trailing) {
                    Button("删除", role: .destructive) {
                        tableToDelete = table
                        showDeleteAlert = true
                    }
                    Button("重命名") {
                        renameText = table.name
                        tableToDelete = table
                        showRenameAlert = true
                    }
                    .tint(.orange)
                    Button("复制") {
                        vm.duplicateTable(table)
                    }
                    .tint(.blue)
                }
            }
        }
        .navigationTitle("课表管理")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showCreateAlert = true } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .onAppear { vm.load() }
        .alert("新建课表", isPresented: $showCreateAlert) {
            TextField("课表名称", text: $newTableName)
            Button("取消", role: .cancel) {}
            Button("创建") {
                if !newTableName.trimmingCharacters(in: .whitespaces).isEmpty {
                    vm.createTable(name: newTableName)
                    newTableName = ""
                }
            }
        } message: {
            Text("输入新课表的名称")
        }
        .alert("确认删除", isPresented: $showDeleteAlert) {
            Button("取消", role: .cancel) {}
            Button("删除", role: .destructive) {
                if let table = tableToDelete {
                    vm.deleteTable(table)
                }
            }
        } message: {
            Text("删除课表将同时删除其中所有课程和配置，此操作不可撤销。")
        }
        .alert("重命名课表", isPresented: $showRenameAlert) {
            TextField("名称", text: $renameText)
            Button("取消", role: .cancel) {}
            Button("确定") {
                if let table = tableToDelete, !renameText.trimmingCharacters(in: .whitespaces).isEmpty {
                    vm.renameTable(table, newName: renameText)
                }
            }
        }
    }
}
