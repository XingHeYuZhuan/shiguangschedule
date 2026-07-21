import SwiftUI

struct ContentView: View {
    @State private var selectedTab: Int = {
        let startScreen = AppSettings.shared.startScreenValue
        return startScreen == .todaySchedule ? 1 : 0
    }()
    @State private var needsRefresh = false

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                WeeklyScheduleView()
            }
            .tabItem {
                Label("课程表", systemImage: "calendar")
            }
            .tag(0)

            NavigationStack {
                TodayScheduleView()
            }
            .tabItem {
                Label("今日", systemImage: "clock.fill")
            }
            .tag(1)

            NavigationStack {
                SettingsView()
            }
            .tabItem {
                Label("设置", systemImage: "gearshape.fill")
            }
            .tag(2)
        }
        .tint(.accentColor)
    }
}
