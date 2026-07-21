import SwiftUI
import SwiftData

@main
struct ShiguangScheduleApp: App {
    let dbService = DatabaseService.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .modelContainer(dbService.container)
                .onAppear {
                    dbService.setupDefaultDataIfNeeded()
                }
        }
    }
}
