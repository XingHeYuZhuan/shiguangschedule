import SwiftUI

struct SettingsView: View {
    @State private var vm = SettingsViewModel()

    var body: some View {
        List {
            scheduleConfigSection
            appearanceSection
            dataSection
            aboutSection
        }
        .navigationTitle("设置")
        .onAppear { vm.load() }
    }

    private var scheduleConfigSection: some View {
        Section("课表配置") {
            NavigationLink("课程管理") {
                CourseManagementView()
            }
            NavigationLink("节次管理") {
                TimeSlotManagementView()
            }
            NavigationLink("课表管理") {
                CourseTableManagementView()
            }

            DatePicker(
                "学期开始",
                selection: Binding(
                    get: { vm.semesterStartDate },
                    set: { vm.semesterStartDate = $0 }
                ),
                displayedComponents: .date
            )

            Stepper(
                "总周数: \(vm.semesterTotalWeeks)",
                value: Binding(
                    get: { vm.semesterTotalWeeks },
                    set: { vm.semesterTotalWeeks = $0 }
                ),
                in: 1...52
            )

            Toggle("显示周末", isOn: Binding(
                get: { vm.showWeekends },
                set: { vm.showWeekends = $0 }
            ))
        }
    }

    private var appearanceSection: some View {
        Section("外观") {
            NavigationLink("主题设置") {
                ThemeSettingsView()
            }
            NavigationLink("样式设置") {
                StyleSettingsView()
            }

            Picker("启动屏幕", selection: Binding(
                get: { vm.settings.startScreenValue },
                set: { vm.settings.startScreenValue = $0 }
            )) {
                ForEach(StartScreen.allCases, id: \.self) { screen in
                    Text(screen.displayName).tag(screen)
                }
            }

            Picker("主题模式", selection: Binding(
                get: { vm.settings.themeModeValue },
                set: { vm.settings.themeModeValue = $0 }
            )) {
                ForEach(AppThemeMode.allCases, id: \.self) { mode in
                    Text(mode.displayName).tag(mode)
                }
            }

            Toggle("动态颜色", isOn: $vm.settings.useDynamicColor)
        }
    }

    private var dataSection: some View {
        Section("数据") {
            NavigationLink("备份与恢复") {
                BackupRestoreView()
            }

            Toggle("课前提醒", isOn: $vm.settings.reminderEnabled)

            if vm.settings.reminderEnabled {
                Stepper(
                    "提前 \(vm.settings.remindBeforeMinutes) 分钟",
                    value: $vm.settings.remindBeforeMinutes,
                    in: 1...60
                )
            }
        }
    }

    private var aboutSection: some View {
        Section("关于") {
            HStack {
                Text("版本")
                Spacer()
                Text("1.0.0")
                    .foregroundColor(.secondary)
            }
            Link("GitHub 仓库", destination: URL(string: "https://github.com/XingHeYuZhuan/shiguangschedule")!)
        }
    }
}
