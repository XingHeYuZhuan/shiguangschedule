import Foundation
import SwiftUI

enum AutoControlMode: String, Codable, CaseIterable {
    case doNotDisturb = "DND"
    case silent

    var displayName: String {
        switch self {
        case .doNotDisturb: return "免打扰"
        case .silent: return "静音"
        }
    }
}

enum StartScreen: String, Codable, CaseIterable {
    case courseSchedule = "COURSE_SCHEDULE"
    case todaySchedule = "TODAY_SCHEDULE"

    var displayName: String {
        switch self {
        case .courseSchedule: return "课程表"
        case .todaySchedule: return "今日课程"
        }
    }
}

enum AppThemeMode: String, Codable, CaseIterable {
    case followSystem = "FOLLOW_SYSTEM"
    case light = "LIGHT"
    case dark = "DARK"

    var displayName: String {
        switch self {
        case .followSystem: return "跟随系统"
        case .light: return "浅色"
        case .dark: return "深色"
        }
    }
}

class AppSettings: ObservableObject {
    static let shared = AppSettings()

    @AppStorage("currentCourseTableId") var currentCourseTableId: String = ""
    @AppStorage("reminderEnabled") var reminderEnabled: Bool = false
    @AppStorage("remindBeforeMinutes") var remindBeforeMinutes: Int = 15
    @AppStorage("skippedDatesData") private var skippedDatesData: String = ""
    @AppStorage("autoModeEnabled") var autoModeEnabled: Bool = false
    @AppStorage("autoControlMode") var autoControlMode: String = AutoControlMode.doNotDisturb.rawValue
    @AppStorage("compatWearableSync") var compatWearableSync: Bool = false
    @AppStorage("showNonCurrentWeekCourses") var showNonCurrentWeekCourses: Bool = false
    @AppStorage("startScreen") var startScreen: String = StartScreen.courseSchedule.rawValue
    @AppStorage("themeMode") var themeMode: String = AppThemeMode.followSystem.rawValue
    @AppStorage("useDynamicColor") var useDynamicColor: Bool = true
    @AppStorage("customLightPrimaryHex") var customLightPrimaryHex: String = ""
    @AppStorage("customDarkPrimaryHex") var customDarkPrimaryHex: String = ""

    var skippedDates: Set<String> {
        get {
            guard let data = skippedDatesData.data(using: .utf8),
                  let set = try? JSONDecoder().decode(Set<String>.self, from: data) else {
                return []
            }
            return set
        }
        set {
            if let data = try? JSONEncoder().encode(newValue),
               let str = String(data: data, encoding: .utf8) {
                skippedDatesData = str
            }
        }
    }

    var autoControlModeValue: AutoControlMode {
        get { AutoControlMode(rawValue: autoControlMode) ?? .doNotDisturb }
        set { autoControlMode = newValue.rawValue }
    }

    var startScreenValue: StartScreen {
        get { StartScreen(rawValue: startScreen) ?? .courseSchedule }
        set { startScreen = newValue.rawValue }
    }

    var themeModeValue: AppThemeMode {
        get { AppThemeMode(rawValue: themeMode) ?? .followSystem }
        set { themeMode = newValue.rawValue }
    }

    func toggleSkippedDate(_ date: String) {
        var dates = skippedDates
        if dates.contains(date) {
            dates.remove(date)
        } else {
            dates.insert(date)
        }
        skippedDates = dates
    }
}
